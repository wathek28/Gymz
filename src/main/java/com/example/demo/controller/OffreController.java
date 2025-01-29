package com.example.demo.controller;

import com.example.demo.model.Offre;
import com.example.demo.service.OffreService;
import com.example.demo.service.JwtTokenService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;

import java.util.List;

@RestController
@RequestMapping("/api/offres")
public class OffreController {
    private static final Logger logger = LoggerFactory.getLogger(OffreController.class);

    @Autowired
    private OffreService offreService;

    @Autowired
    private JwtTokenService jwtTokenService;

    @PostMapping("/creer")
    public ResponseEntity<?> creerOffre(
            @RequestBody OffreService.OffreDto offreDto,
            HttpServletRequest request
    ) {
        try {
            String phoneNumber = extractPhoneNumberFromToken(request);
            validateOffreDto(offreDto);
            Offre offre = offreService.creerOffre(phoneNumber, offreDto);
            return ResponseEntity.ok(offre);
        } catch (IllegalArgumentException e) {
            logger.warn("Erreur de validation: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            logger.error("Erreur création offre", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Une erreur s'est produite."));
        }
    }

    // Méthode d'extraction du numéro de téléphone depuis le token
    private String extractPhoneNumberFromToken(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new IllegalArgumentException("Token d'authentification manquant ou invalide");
        }

        String token = authHeader.substring(7);
        return jwtTokenService.extractPhoneNumber(token);
    }

    // Validation des données de l'offre
    private void validateOffreDto(OffreService.OffreDto offreDto) {
        if (offreDto.getTitre() == null || offreDto.getTitre().trim().isEmpty()) {
            throw new IllegalArgumentException("Le titre de l'offre est obligatoire");
        }
        if (offreDto.getPrix() == null || offreDto.getPrix() <= 0) {
            throw new IllegalArgumentException("Le prix doit être un nombre positif");
        }
    }


    @GetMapping
    public ResponseEntity<?> getAllOffres() {
        try {
            List<Offre> offres = offreService.getAllOffres();
            return ResponseEntity.ok(offres);
        } catch (Exception e) {
            logger.error("Erreur récupération des offres", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Erreur lors de la récupération des offres"));
        }
    }
    @GetMapping("/{id}")
    public ResponseEntity<?> getOffreById(@PathVariable Long id) {
        try {
            Offre offre = offreService.getOffreById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Offre non trouvée"));
            return ResponseEntity.ok(offre);
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            logger.error("Erreur récupération offre", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Erreur lors de la récupération de l'offre"));
        }
    }

    // Ajouter cette classe d'exception
    public class ResourceNotFoundException extends RuntimeException {
        public ResourceNotFoundException(String message) {
            super(message);
        }
    }

    // Changer la route pour éviter le conflit
    @GetMapping("/mes-offres")
    public ResponseEntity<?> getOffresUtilisateur(HttpServletRequest request) {
        try {
            String phoneNumber = extractPhoneNumberFromToken(request);
            List<Offre> offres = offreService.getOffresUtilisateur(phoneNumber);
            return ResponseEntity.ok(offres);
        } catch (Exception e) {
            logger.error("Erreur récupération offres utilisateur", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Erreur lors de la récupération des offres"));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> supprimerOffre(@PathVariable Long id, HttpServletRequest request) {
        try {
            String phoneNumber = extractPhoneNumberFromToken(request);
            Offre offre = offreService.getOffreById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Offre non trouvée"));

            // Vérifier que l'utilisateur est le propriétaire
            if (!offre.getCreateur().getPhoneNumber().equals(phoneNumber)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(new ErrorResponse("Vous n'êtes pas autorisé à supprimer cette offre"));
            }

            offreService.supprimerOffre(id);
            return ResponseEntity.ok().build();
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Erreur lors de la suppression"));
        }
    }




    // Classe de réponse d'erreur
    public static class ErrorResponse {
        private String message;

        public ErrorResponse(String message) {
            this.message = message;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }
    public static class OffreDto {
        private String titre;
        private String description;
        private Double prix;
        private Double pourcentageReduction;
        private String codePromo;

        // Getters et setters
        public String getTitre() { return titre; }
        public void setTitre(String titre) { this.titre = titre; }

        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }

        public Double getPrix() { return prix; }
        public void setPrix(Double prix) { this.prix = prix; }

        public Double getPourcentageReduction() { return pourcentageReduction; }
        public void setPourcentageReduction(Double pourcentageReduction) {
            this.pourcentageReduction = pourcentageReduction;
        }

        public String getCodePromo() { return codePromo; }
        public void setCodePromo(String codePromo) { this.codePromo = codePromo; }
    }

}
