package com.example.demo.controller;

import com.example.demo.model.Role;
import com.example.demo.model.User;
import com.example.demo.service.JwtTokenService;
import com.example.demo.service.UserService;
import io.jsonwebtoken.io.IOException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


@RestController
@RequestMapping("/api/auth")
@Slf4j
@RequiredArgsConstructor
public class AuthController {

    private final JwtTokenService jwtTokenService;
    private final UserService userService;


    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@RequestBody @Valid PhoneNumberRequest request) {
        try {
            String code = userService.sendVerificationCode(request.getPhoneNumber(), String.valueOf(request.getRole()));
            log.info("Code de vérification envoyé: {}", request.getPhoneNumber());
            return ResponseEntity.ok(new CodeResponse(code));
        } catch (IllegalArgumentException e) {
            log.warn("Erreur d'enregistrement: {}", e.getMessage());
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }

    @PostMapping("/verify")
    public ResponseEntity<?> verifyPhoneNumber(@RequestBody @Valid VerificationRequest request) {
        try {
            String token = userService.verifyPhoneNumber(request.getPhoneNumber(), request.getVerificationCode());
            return token != null ? ResponseEntity.ok(new TokenResponse(token))
                    : ResponseEntity.badRequest().body(new ErrorResponse("Code incorrect"));
        } catch (Exception e) {
            log.error("Erreur de vérification: {}", e.getMessage());
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody @Valid LoginRequest request) {
        try {
            String loginCode = userService.sendLoginCode(request.getPhoneNumber());
            return ResponseEntity.ok(new CodeResponse(loginCode));
        } catch (Exception e) {
            log.error("Erreur de login: {}", e.getMessage());
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }

    @PostMapping("/verify-login")
    public ResponseEntity<?> verifyLogin(@RequestBody @Valid VerificationRequest request) {
        try {
            // Vérifier le code de connexion
            String token = userService.verifyLoginCode(request.getPhoneNumber(), request.getVerificationCode());

            // Si le token est généré avec succès
            if (token != null) {
                // Récupérer l'utilisateur associé au numéro de téléphone
                User user = userService.findUserByPhoneNumber(request.getPhoneNumber());

                // Créer un map avec le token, l'ID de l'utilisateur, le firstName, le numéro de téléphone, la photo et l'email
                Map<String, Object> response = new HashMap<>();
                response.put("token", token);
                response.put("userId", user.getId());
                response.put("firstName", user.getFirstName());
                response.put("phoneNumber", user.getPhoneNumber());
                response.put("photo", user.getPhoto());
                response.put("email", user.getEmail());  // Ajout de l'email

                return ResponseEntity.ok(response);
            } else {
                // Si le code de vérification est incorrect
                return ResponseEntity.badRequest().body(new ErrorResponse("Code incorrect"));
            }
        } catch (Exception e) {
            log.error("Erreur de vérification login: {}", e.getMessage());
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }



    @GetMapping("/checkPhoneNumber")
    public ResponseEntity<?> checkPhoneNumber(@RequestParam @NotBlank String phoneNumber) {
        try {
            boolean exists = userService.checkPhoneNumberExists(phoneNumber);
            return ResponseEntity.ok(new CheckPhoneResponse(exists));
        } catch (Exception e) {
            log.error("Erreur de vérification du numéro: {}", e.getMessage());
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }

    @GetMapping("/coaches")
    public ResponseEntity<List<UserService.CoachProfileDto>> getAllCoaches(
            @RequestParam(required = false) String userId) {

        if (userId != null) {
            // Exemple de validation basique si vous attendez un ID numérique
            if (!userId.matches("\\d+")) {
                return ResponseEntity.badRequest().body(null); // Retourner une erreur 400 si l'ID n'est pas valide
            }
            System.out.println("User ID: " + userId);
        }

        List<UserService.CoachProfileDto> coaches = userService.getAllCoachProfiles();

        if (coaches.isEmpty()) {
            return ResponseEntity.noContent().build(); // Retourner un statut 204 si aucune donnée
        }

        return ResponseEntity.ok(coaches);
    }


    @GetMapping("/gyms")
    public ResponseEntity<List<UserService.GymProfileDto>> getAllGyms() {
        try {
            // Récupération des gyms et des informations supplémentaires (type de coaching et bio)
            List<UserService.GymProfileDto> gyms = userService.getAllGyms().stream()
                    .map(user -> new UserService.GymProfileDto(
                            user,
                            user.getTypeCoaching(), // Récupérer le type de coaching
                            user.getBio()           // Récupérer la biographie
                    ))
                    .collect(Collectors.toList()); // Utiliser collect pour bien transformer le stream en liste

            if (gyms.isEmpty()) {
                return ResponseEntity.noContent().build(); // Retourner un statut 204 si aucune donnée
            }

            return ResponseEntity.ok(gyms); // Retourner la liste des gyms avec les informations
        } catch (Exception e) {
            log.error("Erreur de récupération des gyms: {}", e.getMessage());
            return ResponseEntity.internalServerError().build(); // Retourner une erreur 500 si une exception est levée
        }
    }







    @GetMapping("/coaches/{id}")
        public ResponseEntity<UserService.CoachProfileDto> getCoachById(@PathVariable Long id) {
            return ResponseEntity.ok(userService.getCoachById(id));
        }




    @PostMapping("/update-profile")
    public ResponseEntity<?> updateProfile(
            @RequestHeader("Authorization") String token,
            @ModelAttribute @Valid ProfileUpdateRequest request
    ) {
        try {
            // Validate the token first
            String phoneNumber = jwtTokenService.extractPhoneNumber(token.substring(7)); // Remove "Bearer "

            UserService.ProfileUpdateDto profileDto = new UserService.ProfileUpdateDto();
            profileDto.setFirstName(request.getFirstName());
            profileDto.setEmail(request.getEmail());

            // Handle photo upload
            if (request.getPhoto() != null) {
                profileDto.setPhoto(request.getPhoto().getBytes());
            }

            UserService.UserProfileDto updatedProfile = userService.updateProfile(phoneNumber, profileDto);
            return ResponseEntity.ok(updatedProfile);
        } catch (Exception e) {
            log.error("Erreur de mise à jour du profil: {}", e.getMessage());
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }


    @Data
    public static class PhoneNumberRequest {
        @NotBlank(message = "Le numéro de téléphone est requis")
        private String phoneNumber;
        @NotNull(message = "Le rôle est requis")
        private Role role;
    }

    @Data
    public static class LoginRequest {
        @NotBlank(message = "Le numéro de téléphone est requis")
        private String phoneNumber;
    }

    @Data
    public static class VerificationRequest {
        @NotBlank(message = "Le numéro de téléphone est requis")
        private String phoneNumber;
        @NotBlank(message = "Le code de vérification est requis")
        private String verificationCode;
    }

    @Data
    public static class ProfileUpdateRequest {
        private String firstName;
        @Email(message = "Format d'email invalide")
        private String email;
        private MultipartFile photo;
    }

    @Data
    @AllArgsConstructor
    public static class TokenResponse {
        private String token;
    }

    @Data
    @AllArgsConstructor
    public static class CodeResponse {
        private String code;
    }

    @Data
    @AllArgsConstructor
    public static class ErrorResponse {
        private String message;
    }

    @Data
    @AllArgsConstructor
    public static class CheckPhoneResponse {
        private boolean exists;
    }

    /// //// update coach
    @PostMapping("/update-profile-coach")
    public ResponseEntity<?> updateProfileCoach(
            @RequestHeader("Authorization") String token,
            @ModelAttribute @Valid CoachProfileUpdateRequest request
    ) {
        try {
            String phoneNumber = jwtTokenService.extractPhoneNumber(token.substring(7));

            UserService.CoachProfileUpdateDto profileDto = new UserService.CoachProfileUpdateDto();
            profileDto.setFirstName(request.getFirstName());
            profileDto.setBio(request.getBio());
            profileDto.setFb(request.getFb());
            profileDto.setInsta(request.getInsta());
            profileDto.setTiktok(request.getTiktok());
            profileDto.setPoste(request.getPoste());
            profileDto.setDureeExperience(request.getDureeExperience());
            profileDto.setExperiencesProfessionnelles(request.getExperiencesProfessionnelles());
            profileDto.setCertifications(request.getCertifications());
            profileDto.setCompetencesGenerales(request.getCompetencesGenerales());
            profileDto.setEntrainementPhysique(request.getEntrainementPhysique());
            profileDto.setDisciplines(request.getDisciplines());
            profileDto.setSanteEtBienEtre(request.getSanteEtBienEtre());
            profileDto.setCoursSpecifiques(request.getCoursSpecifiques());
            profileDto.setNiveauCours(request.getNiveauCours());
            profileDto.setDureeSeance(request.getDureeSeance());
            profileDto.setPrixSeance(request.getPrixSeance());
            profileDto.setTypeCoaching(request.getTypeCoaching());
            profileDto.setEmail(request.getEmail()); // Ajout du champ email

            if (request.getPhoto() != null) {
                profileDto.setPhoto(request.getPhoto().getBytes());
            }

            UserService.CoachProfileDto updatedProfile = userService.updateProfileCoach(phoneNumber, profileDto);
            return ResponseEntity.ok(updatedProfile);
        } catch (Exception e) {
            log.error("Erreur de mise à jour du profil coach: {}", e.getMessage());
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }

    @Data
    public static class CoachProfileUpdateRequest {
        @Size(min = 2, max = 100)
        private String firstName;
        private String bio;
        private MultipartFile photo;
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

        @Email
        private String email; // Ajout du champ email avec validation
    }

    /// //////////// update profil user
    @PostMapping("/update-photo")
    public ResponseEntity<?> updatePhoto(
            @RequestParam("phoneNumber") String phoneNumber,
            @RequestParam(value = "photo", required = false) MultipartFile photoFile) {
        try {
            UserService.UserProfileDto updatedProfile = userService.updateUserPhoto(phoneNumber, photoFile);
            return ResponseEntity.ok(updatedProfile);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // Dans AuthController.java, ajoutez ces méthodes et classes :

    // Dans AuthController.java, ajoutez ces méthodes modifiées :

    @PostMapping("/initiate-phone-change")
    public ResponseEntity<?> initiatePhoneNumberChange(@RequestBody @Valid PhoneChangeRequestNoToken request) {
        try {
            String code = userService.initiatePhoneNumberChange(
                    request.getCurrentPhoneNumber(),
                    request.getNewPhoneNumber()
            );

            return ResponseEntity.ok(new CodeResponse(code));
        } catch (Exception e) {
            log.error("Erreur lors de l'initiation du changement de numéro: {}", e.getMessage());
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }

    @PostMapping("/confirm-phone-change")
    public ResponseEntity<?> confirmPhoneNumberChange(@RequestBody @Valid PhoneVerificationRequestNoToken request) {
        try {
            UserService.UserProfileDto updatedProfile = userService.confirmPhoneNumberChange(
                    request.getCurrentPhoneNumber(),
                    request.getVerificationCode()
            );

            // Générer un nouveau token avec le nouveau numéro
            String newToken = jwtTokenService.generateToken(userService.findUserByPhoneNumber(updatedProfile.getPhoneNumber()));

            // Créer une réponse avec le token et le profil mis à jour
            Map<String, Object> response = new HashMap<>();
            response.put("token", newToken);
            response.put("profile", updatedProfile);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Erreur lors de la confirmation du changement de numéro: {}", e.getMessage());
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }

    // Classes pour les requêtes sans token
    @Data
    public static class PhoneChangeRequestNoToken {
        @NotBlank(message = "Le numéro de téléphone actuel est requis")
        private String currentPhoneNumber;

        @NotBlank(message = "Le nouveau numéro de téléphone est requis")
        private String newPhoneNumber;
    }

    @Data
    public static class PhoneVerificationRequestNoToken {
        @NotBlank(message = "Le numéro de téléphone actuel est requis")
        private String currentPhoneNumber;

        @NotBlank(message = "Le code de vérification est requis")
        private String verificationCode;
    }

    /// ////
    @PostMapping("/modifier-user")
    public ResponseEntity<?> modifierUser(
            @RequestParam("userId") Long userId,
            @Valid @RequestBody UserService.UserModificationDto userDetails
    ) {
        try {
            // Log the incoming user ID for debugging
            log.info("Attempting to modify user with ID: {}", userId);

            // Appeler le service pour modifier l'utilisateur
            UserService.UserProfileDto updatedProfile = userService.modifierUser(userId, userDetails);

            return ResponseEntity.ok(updatedProfile);
        } catch (IllegalArgumentException e) {
            log.error("User modification error: {}",
                    e.getMessage() != null ? e.getMessage() : "Unknown user modification error");
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "Utilisateur non trouvé"));
        } catch (Exception e) {
            log.error("Unexpected error modifying user", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Une erreur est survenue"));
        }
    }
}