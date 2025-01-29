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
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

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
            String token = userService.verifyLoginCode(request.getPhoneNumber(), request.getVerificationCode());
            return ResponseEntity.ok(new TokenResponse(token));
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
    public ResponseEntity<List<UserService.UserProfileDto>> getAllCoaches() {
        try {
            return ResponseEntity.ok(userService.getAllCoaches().stream()
                    .map(UserService.UserProfileDto::new)
                    .toList());
        } catch (Exception e) {
            log.error("Erreur de récupération des coachs: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/gyms")
    public ResponseEntity<List<UserService.UserProfileDto>> getAllGyms() {
        try {
            return ResponseEntity.ok(userService.getAllGyms().stream()
                    .map(UserService.UserProfileDto::new)
                    .toList());
        } catch (Exception e) {
            log.error("Erreur de récupération des gyms: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
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
}