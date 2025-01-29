package com.example.demo.service;

import com.example.demo.model.Role;
import com.example.demo.model.User;
import com.example.demo.repository.UserRepository;
import io.jsonwebtoken.io.IOException;
import jakarta.transaction.Transactional;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.multipart.MultipartFile;

import java.security.SecureRandom;
import java.util.List;
import java.util.Optional;

@Service
public class UserService {
    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

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

    private User findUserByPhoneNumber(String phoneNumber) {
        return userRepository.findByPhoneNumber(phoneNumber)
                .orElseThrow(() -> {
                    logger.warn("Utilisateur non trouvé : {}", phoneNumber);
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
    public List<User> getAllCoaches() {
        return userRepository.findByRole(Role.COACH);
    }

    @Transactional
    public List<User> getAllGyms() {
        return userRepository.findByRole(Role.GYM);
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
}

