package com.example.demo.service;

import com.example.demo.model.Role;
import com.example.demo.model.User;
import com.example.demo.repository.UserRepository;
import jakarta.transaction.Transactional;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.multipart.MultipartFile;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;


@Service
public class UserService {
    public static final Logger logger = LoggerFactory.getLogger(UserService.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TwilioService twilioService;

    @Autowired
    private JwtTokenService jwtTokenService;

    private static final String GYMZER = "GYMZER";
    private static final String ADMIN = "ADMIN";
    private static final String GYM = "GYM";
    private static final String COACH = "COACH";


    @Transactional
    public String sendVerificationCode(String phoneNumber, String roleString) {
        String formattedPhoneNumber = formatPhoneNumber(phoneNumber);
        Role role = validateRole(roleString);

        Optional<User> existingUser = userRepository.findByPhoneNumber(formattedPhoneNumber);
        if (existingUser.isPresent() && existingUser.get().isVerified()) {
            logger.warn("Le numéro {} est déjà vérifié.", formattedPhoneNumber);
            throw new IllegalArgumentException("Ce numéro est déjà vérifié.");
        }

        String verificationCode = generateVerificationCode();
        User user = existingUser.orElseGet(User::new);
        user.setPhoneNumber(formattedPhoneNumber);
        user.setVerificationCode(verificationCode);
        user.setVerified(false);
        user.setRole(role);
        userRepository.save(user);

        twilioService.sendSMS(formattedPhoneNumber, "Votre code de vérification : " + verificationCode);
        logger.info("Code envoyé au : {} avec le rôle : {}", formattedPhoneNumber, role);
        return verificationCode;
    }

    private Role validateRole(String roleString) {
        try {
            return Role.fromString(roleString);
        } catch (IllegalArgumentException e) {
            logger.warn("Rôle invalide : {}", roleString);
            throw new IllegalArgumentException("Rôle invalide. Acceptés : " + GYMZER + ", " + ADMIN + ", " + GYM + ", " + COACH);
        }
    }

    public String verifyPhoneNumber(String phoneNumber, String verificationCode) {
        String formattedPhoneNumber = formatPhoneNumber(phoneNumber);
        User user = findUserByPhoneNumber(formattedPhoneNumber);

        if (!verificationCode.equals(user.getVerificationCode())) {
            logger.warn("Code incorrect pour : {}", formattedPhoneNumber);
            return null;
        }

        user.setVerified(true);
        user.setVerificationCode(null);
        userRepository.save(user);
        logger.info("Numéro {} vérifié", formattedPhoneNumber);
        return jwtTokenService.generateToken(user);
    }

    public User findUserByPhoneNumber(String phoneNumber) {
        logger.info("Searching for user with phone number: {}", phoneNumber);

        Optional<User> user = userRepository.findByPhoneNumber(phoneNumber);
        logger.info("User found with exact match: {}", user.isPresent());

        if (user.isEmpty() && phoneNumber.startsWith("+")) {
            user = userRepository.findByPhoneNumber(phoneNumber.substring(1));
            logger.info("User found after removing '+': {}", user.isPresent());
        }

        if (user.isEmpty() && !phoneNumber.startsWith("+")) {
            user = userRepository.findByPhoneNumber("+" + phoneNumber);
            logger.info("User found after adding '+': {}", user.isPresent());
        }

        return user.orElseThrow(() -> {
            logger.warn("No user found for phone number: {}", phoneNumber);
            return new IllegalArgumentException("Utilisateur non trouvé");
        });
    }

    private String generateVerificationCode() {
        return String.valueOf(100000 + new SecureRandom().nextInt(900000));
    }

    public String formatPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
            logger.error("Numéro vide");
            throw new IllegalArgumentException("Numéro invalide");
        }

        phoneNumber = phoneNumber.replaceAll("[^\\d+]", "");
        if (!phoneNumber.matches("^\\+?\\d{1,3}\\d{4,14}$")) {
            logger.error("Format invalide : {}", phoneNumber);
            throw new IllegalArgumentException("Numéro invalide");
        }

        return phoneNumber;
    }

    public boolean checkPhoneNumberExists(String phoneNumber) {
        String normalized = phoneNumber.replaceAll("^\\+", "");
        boolean exists = userRepository.existsByPhoneNumber(normalized);
        logger.info("Numéro {} existe: {}", normalized, exists);
        return exists;
    }

    public String generateTokenAfterVerification(String phoneNumber) {
        User user = findUserByPhoneNumber(phoneNumber);
        if (!user.isVerified()) {
            throw new IllegalStateException("Utilisateur non vérifié");
        }
        return jwtTokenService.generateToken(user);
    }

    public String sendLoginCode(String phoneNumber) {
        User user = findUserByPhoneNumber(phoneNumber);
        if (!user.isVerified()) {
            throw new IllegalArgumentException("Compte non vérifié");
        }

        String loginCode = generateVerificationCode();
        user.setVerificationCode(loginCode);
        userRepository.save(user);
        twilioService.sendSMS(phoneNumber, "Code de connexion: " + loginCode);
        return loginCode;
    }

    public String verifyLoginCode(String phoneNumber, String code) {
        User user = findUserByPhoneNumber(phoneNumber);
        if (!code.equals(user.getVerificationCode())) {
            throw new IllegalArgumentException("Code invalide");
        }
        user.setVerificationCode(null);
        userRepository.save(user);
        return jwtTokenService.generateToken(user);
    }

    @Transactional
    public UserProfileDto updateProfile(String phoneNumber, ProfileUpdateDto profileDto) {
        User user = findUserByPhoneNumber(phoneNumber);

        // Add verification check using isVerified
        if (!user.isVerified()) {
            throw new IllegalStateException("Profil non vérifié");
        }

        if (profileDto.getFirstName() != null) {
            user.setFirstName(profileDto.getFirstName());
        }
        if (profileDto.getEmail() != null) {
            user.setEmail(profileDto.getEmail());
        }
        if (profileDto.getPhoto() != null) {
            user.setPhoto(profileDto.getPhoto());
        } else {
            user.setPhoto(null);
        }

        return new UserProfileDto(userRepository.save(user));
    }



    @Getter
    @Setter
    public static class ProfileUpdateDto {
        private String firstName;
        private String email;
        private byte[] photo;
    }

    @Transactional
    public List<CoachProfileDto> getAllCoachProfiles() {
        List<User> coaches = userRepository.findByRole(Role.COACH);
        return coaches.stream()
                .map(CoachProfileDto::new)
                .collect(Collectors.toList());
    }


    @Transactional
    public List<User> getAllGyms() {
        return userRepository.findByRole(Role.GYM);
    }
    @Transactional
    public CoachProfileDto getCoachById(Long id) {
        User coach = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Coach non trouvé avec l'ID : " + id));

        if (coach.getRole() != Role.COACH) {
            throw new IllegalArgumentException("L'utilisateur avec l'ID " + id + " n'est pas un coach.");
        }

        return new CoachProfileDto(coach);
    }



    @Setter
    @Getter
    public static class UserProfileDto {
        private final String phoneNumber;
        private final String firstName;
        private final String email;
        private final Role role;
        private final byte[] photo;

        public UserProfileDto(User user) {
            this.phoneNumber = user.getPhoneNumber();
            this.firstName = user.getFirstName();
            this.email = user.getEmail();
            this.role = user.getRole();
            this.photo = user.getPhoto();
        }
    }

    /// //// update coach
    @Getter
    @Setter
    public static class CoachProfileUpdateDto {
        private String firstName;
        private String bio;
        private byte[] photo;
        private String fb;
        private String insta;
        private String tiktok;
        private String poste;
        private String dureeExperience;
        private String experiencesProfessionnelles;
        private String certifications;
        private String competencesGenerales;
        private String entrainementPhysique;
        private String disciplines;
        private String santeEtBienEtre;
        private String coursSpecifiques;
        private String niveauCours;
        private String dureeSeance;
        private String prixSeance;
        private String typeCoaching;
        private String email; // Ajout du champ email
    }

    @Getter
    @Setter
    public static class CoachProfileDto {
        private final Long id;

        private final String phoneNumber;
        private final String firstName;
        private final String bio;
        private final byte[] photo;
        private final String fb;
        private final String insta;
        private final String tiktok;
        private final String poste;
        private final String dureeExperience;
        private final String experiencesProfessionnelles;
        private final String certifications;
        private final String competencesGenerales;
        private final String entrainementPhysique;
        private final String disciplines;
        private final String santeEtBienEtre;
        private final String coursSpecifiques;
        private final String niveauCours;
        private final String dureeSeance;
        private final String prixSeance;
        private final String typeCoaching;
        private final String email; // Ajout du champ email

        public CoachProfileDto(User user) {
            this.id = user.getId();
            this.phoneNumber = user.getPhoneNumber();
            this.firstName = user.getFirstName();
            this.bio = user.getBio();
            this.photo = user.getPhoto();
            this.fb = user.getFb();
            this.insta = user.getInsta();
            this.tiktok = user.getTiktok();
            this.poste = user.getPoste();
            this.dureeExperience = user.getDureeExperience();
            this.experiencesProfessionnelles = user.getExperiencesProfessionnelles();
            this.certifications = user.getCertifications();
            this.competencesGenerales = user.getCompetencesGenerales();
            this.entrainementPhysique = user.getEntrainementPhysique();
            this.disciplines = user.getDisciplines();
            this.santeEtBienEtre = user.getSanteEtBienEtre();
            this.coursSpecifiques = user.getCoursSpecifiques();
            this.niveauCours = user.getNiveauCours();
            this.dureeSeance = user.getDureeSeance();
            this.prixSeance = user.getPrixSeance();
            this.typeCoaching = user.getTypeCoaching();
            this.email = user.getEmail(); // Ajout du champ email

        }
    }

    @Transactional
    public CoachProfileDto updateProfileCoach(String phoneNumber, CoachProfileUpdateDto profileDto) {
        User user = findUserByPhoneNumber(phoneNumber);

        if (!user.isVerified()) {
            logger.warn("Tentative de mise à jour d'un profil non vérifié : {}", phoneNumber);
            throw new IllegalStateException("Profil non vérifié");
        }

        if (user.getRole() != Role.COACH) {
            logger.warn("Tentative de mise à jour d'un profil coach par un non-coach : {}", phoneNumber);
            throw new IllegalStateException("L'utilisateur n'est pas un coach");
        }

        if (profileDto.getFirstName() != null) {
            user.setFirstName(profileDto.getFirstName());
        }
        if (profileDto.getBio() != null) {
            user.setBio(profileDto.getBio());
        }
        if (profileDto.getPhoto() != null) {
            user.setPhoto(profileDto.getPhoto());
        }
        if (profileDto.getFb() != null) {
            user.setFb(profileDto.getFb());
        }
        if (profileDto.getInsta() != null) {
            user.setInsta(profileDto.getInsta());
        }
        if (profileDto.getTiktok() != null) {
            user.setTiktok(profileDto.getTiktok());
        }
        if (profileDto.getPoste() != null) {
            user.setPoste(profileDto.getPoste());
        }
        if (profileDto.getDureeExperience() != null) {
            user.setDureeExperience(profileDto.getDureeExperience());
        }
        if (profileDto.getExperiencesProfessionnelles() != null) {
            user.setExperiencesProfessionnelles(profileDto.getExperiencesProfessionnelles());
        }
        if (profileDto.getCertifications() != null) {
            user.setCertifications(profileDto.getCertifications());
        }
        if (profileDto.getCompetencesGenerales() != null) {
            user.setCompetencesGenerales(profileDto.getCompetencesGenerales());
        }
        if (profileDto.getEntrainementPhysique() != null) {
            user.setEntrainementPhysique(profileDto.getEntrainementPhysique());
        }
        if (profileDto.getDisciplines() != null) {
            user.setDisciplines(profileDto.getDisciplines());
        }
        if (profileDto.getSanteEtBienEtre() != null) {
            user.setSanteEtBienEtre(profileDto.getSanteEtBienEtre());
        }
        if (profileDto.getCoursSpecifiques() != null) {
            user.setCoursSpecifiques(profileDto.getCoursSpecifiques());
        }
        if (profileDto.getNiveauCours() != null) {
            user.setNiveauCours(profileDto.getNiveauCours());
        }
        if (profileDto.getDureeSeance() != null) {
            user.setDureeSeance(profileDto.getDureeSeance());
        }
        if (profileDto.getPrixSeance() != null) {
            user.setPrixSeance(profileDto.getPrixSeance());
        }
        if (profileDto.getTypeCoaching() != null) {
            user.setTypeCoaching(profileDto.getTypeCoaching());
        }
        if (profileDto.getEmail() != null) {
            user.setEmail(profileDto.getEmail()); // Mise à jour de l'email
        }

        logger.info("Mise à jour du profil coach pour : {}", phoneNumber);
        return new CoachProfileDto(userRepository.save(user));
    }
    ////////////comentaire


    @Autowired
    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    // Méthode pour trouver un utilisateur par son ID
    public Optional<User> getUserById(Long id) {
        return userRepository.findById(id);
    }

    // Autres méthodes pour gérer les utilisateurs
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public User saveUser(User user) {
        return userRepository.save(user);
    }

    public void deleteUser(Long id) {
        userRepository.deleteById(id);
    }

    /////////// upodate profil user
    @Transactional
    public UserProfileDto updateUserPhoto(String phoneNumber, MultipartFile photoFile) {
        User user = findUserByPhoneNumber(phoneNumber);

        if (!user.isVerified()) {
            logger.warn("Tentative de mise à jour d'une photo par un utilisateur non vérifié : {}", phoneNumber);
            throw new IllegalStateException("Utilisateur non vérifié");
        }

        try {
            if (photoFile != null && !photoFile.isEmpty()) {
                user.setPhoto(photoFile.getBytes());
                logger.info("Photo mise à jour pour l'utilisateur : {}", phoneNumber);
            } else {
                user.setPhoto(null);
                logger.info("Photo supprimée pour l'utilisateur : {}", phoneNumber);
            }

            userRepository.save(user);
            return new UserProfileDto(user);
        } catch (java.io.IOException e) {
            logger.error("Erreur lors du traitement de la photo pour l'utilisateur {} : {}", phoneNumber, e.getMessage());
            throw new RuntimeException("Erreur lors du traitement de la photo", e);
        }
    }


    // Dans UserService.java, ajoutez ces méthodes :

// Dans UserService.java, ajoutez ces méthodes modifiées :

    @Transactional
    public String initiatePhoneNumberChange(String currentPhoneNumber, String newPhoneNumber) {
        // Vérifier que l'utilisateur existe
        User user = findUserByPhoneNumber(currentPhoneNumber);

        // Vérifier que le nouveau numéro n'est pas déjà utilisé
        String formattedNewPhoneNumber = formatPhoneNumber(newPhoneNumber);
        if (userRepository.existsByPhoneNumber(formattedNewPhoneNumber)) {
            logger.warn("Tentative de changement vers un numéro déjà utilisé : {}", formattedNewPhoneNumber);
            throw new IllegalArgumentException("Ce numéro est déjà utilisé par un autre compte");
        }

        // Générer un code de vérification
        String verificationCode = generateVerificationCode();

        // Stocker temporairement le nouveau numéro et le code dans l'entité utilisateur
        user.setTempPhoneNumber(formattedNewPhoneNumber);
        user.setVerificationCode(verificationCode);
        userRepository.save(user);

        // Envoyer le code de vérification au nouveau numéro
        twilioService.sendSMS(formattedNewPhoneNumber, "Votre code de vérification pour changer de numéro : " + verificationCode);

        logger.info("Code de vérification envoyé au nouveau numéro : {}", formattedNewPhoneNumber);
        return verificationCode;
    }

    @Transactional
    public UserProfileDto confirmPhoneNumberChange(String currentPhoneNumber, String verificationCode) {
        // Vérifier que l'utilisateur existe
        User user = findUserByPhoneNumber(currentPhoneNumber);

        // Vérifier que l'utilisateur a un numéro temporaire en attente
        if (user.getTempPhoneNumber() == null) {
            logger.warn("Tentative de confirmation sans changement initié : {}", currentPhoneNumber);
            throw new IllegalStateException("Aucun changement de numéro n'a été initié");
        }

        // Vérifier le code
        if (!verificationCode.equals(user.getVerificationCode())) {
            logger.warn("Code de vérification incorrect pour le changement de numéro : {}", currentPhoneNumber);
            throw new IllegalArgumentException("Code de vérification incorrect");
        }

        // Mettre à jour le numéro de téléphone
        String newPhoneNumber = user.getTempPhoneNumber();
        user.setPhoneNumber(newPhoneNumber);
        user.setTempPhoneNumber(null);
        user.setVerificationCode(null);
        userRepository.save(user);

        logger.info("Numéro de téléphone changé avec succès de {} à {}", currentPhoneNumber, newPhoneNumber);
        return new UserProfileDto(user);
    }



    @Transactional
    public UserProfileDto modifierUser(Long userId, UserModificationDto modificationDto) {
        // Find user by ID instead of phone number
        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    logger.warn("Utilisateur non trouvé avec l'ID : {}", userId);
                    return new IllegalArgumentException("Utilisateur non trouvé");
                });

        // Vérifier que l'utilisateur est vérifié
        if (!user.isVerified()) {
            logger.warn("Tentative de modification d'un profil non vérifié : {}", userId);
            throw new IllegalStateException("Profil non vérifié");
        }

        // Mettre à jour le prénom
        if (modificationDto.getFirstName() != null) {
            user.setFirstName(modificationDto.getFirstName());
        }

        // Mettre à jour l'email
        if (modificationDto.getEmail() != null) {
            user.setEmail(modificationDto.getEmail());
        }

        // Mettre à jour la date de naissance
        if (modificationDto.getBirthDate() != null && !modificationDto.getBirthDate().isEmpty()) {
            try {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
                LocalDate birthDate = LocalDate.parse(modificationDto.getBirthDate(), formatter);
                user.setBirthDate(birthDate);
            } catch (DateTimeParseException e) {
                logger.error("Format de date invalide : {}", modificationDto.getBirthDate());
                throw new IllegalArgumentException("Format de date invalide. Utilisez le format JJ/MM/AAAA.");
            }
        }

        // Mettre à jour l'adresse
        if (modificationDto.getAddress() != null) {
            user.setAddress(modificationDto.getAddress());
        }

        // Enregistrer les modifications
        User updatedUser = userRepository.save(user);
        logger.info("Profil utilisateur mis à jour pour l'utilisateur ID: {}", userId);

        return new UserProfileDto(updatedUser);
    }

    @Getter
    @Setter
    public static class UserModificationDto {
        @Size(min = 2, max = 50, message = "Le prénom doit contenir entre 2 et 50 caractères")
        private String firstName;

        @Email(message = "Format d'email invalide")
        private String email;

        @Pattern(regexp = "^\\d{2}/\\d{2}/\\d{4}$", message = "La date de naissance doit être au format JJ/MM/AAAA")
        private String birthDate; // Changed from birthDateStr

        @Size(max = 200, message = "L'adresse ne peut pas dépasser 200 caractères")
        private String address;
    }

    //////////gym
    @Data
    @AllArgsConstructor
    public static class GymProfileDto {
        private Long id;
        private String firstName;
        private String phoneNumber;
        private String email;
        private byte[] photo;
        private String typeCoaching;
        private String bio;
        private String address; // New field for address

        // Updated constructor
        public GymProfileDto(User user, String typeCoaching, String bio) {
            this.id = user.getId();
            this.firstName = user.getFirstName();
            this.phoneNumber = user.getPhoneNumber();
            this.email = user.getEmail();
            this.photo = user.getPhoto();
            this.typeCoaching = typeCoaching;
            this.bio = bio;
            this.address = user.getAddress(); // Get address from user
        }
    }




}

