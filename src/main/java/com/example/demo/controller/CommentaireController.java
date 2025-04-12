package com.example.demo.controller;

import com.example.demo.model.Commentaire;
import com.example.demo.model.User;
import com.example.demo.service.CommentaireService;
import com.example.demo.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/commentaires")
public class CommentaireController {

    private final CommentaireService commentaireService;
    private final UserService userService;

    @Autowired
    public CommentaireController(CommentaireService commentaireService, UserService userService) {
        this.commentaireService = commentaireService;
        this.userService = userService;
    }

    // Ajouter un commentaire avec les IDs dans l'URL
    @PostMapping(value = "/{utilisateurId}/{recepteurId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> ajouterCommentaire(
            @PathVariable Long utilisateurId,
            @PathVariable Long recepteurId,
            @RequestParam("evaluation") Integer evaluation,
            @RequestParam("commentaire") String commentaire,
            @RequestParam(value = "imageAvant", required = false) MultipartFile imageAvant,
            @RequestParam(value = "imageApres", required = false) MultipartFile imageApres) {

        try {
            // Vérifier si les utilisateurs existent
            Optional<User> utilisateur = userService.getUserById(utilisateurId);
            Optional<User> recepteur = userService.getUserById(recepteurId);

            if (utilisateur.isEmpty() || recepteur.isEmpty()) {
                return ResponseEntity.badRequest().body("Utilisateur ou récepteur non trouvé");
            }

            // Créer un nouveau commentaire
            Commentaire nouveauCommentaire = new Commentaire();
            nouveauCommentaire.setEvaluation(evaluation);
            nouveauCommentaire.setCommentaire(commentaire);
            nouveauCommentaire.setDateCommentaire(LocalDateTime.now());
            nouveauCommentaire.setUtilisateur(utilisateur.get());
            nouveauCommentaire.setRecepteur(recepteur.get());

            // Ajouter les images si elles sont fournies
            if (imageAvant != null && !imageAvant.isEmpty()) {
                nouveauCommentaire.setImageAvant(imageAvant.getBytes());
            }

            if (imageApres != null && !imageApres.isEmpty()) {
                nouveauCommentaire.setImageApres(imageApres.getBytes());
            }

            // Enregistrer le commentaire
            Commentaire commentaireAjoute = commentaireService.ajouterCommentaire(nouveauCommentaire);
            return ResponseEntity.ok(commentaireAjoute);

        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erreur lors du traitement des images: " + e.getMessage());
        }
    }

    // Récupérer les commentaires reçus par un coach
    // Récupérer les commentaires reçus par un coach
    @GetMapping("/recus/{recepteurId}")
    public ResponseEntity<List<Map<String, Object>>> getCommentairesRecus(@PathVariable Long recepteurId) {
        List<Commentaire> commentaires = commentaireService.getCommentairesRecus(recepteurId);
        List<Map<String, Object>> resultList = new ArrayList<>();

        for (Commentaire commentaire : commentaires) {
            Map<String, Object> commentaireMap = new HashMap<>();

            // Informations de base
            commentaireMap.put("id", commentaire.getId());
            commentaireMap.put("commentaire", commentaire.getCommentaire());
            commentaireMap.put("evaluation", commentaire.getEvaluation());
            commentaireMap.put("dateCommentaire", commentaire.getDateCommentaire());

            // Information sur l'utilisateur sans références circulaires
            Map<String, Object> utilisateurMap = new HashMap<>();
            utilisateurMap.put("id", commentaire.getUtilisateur().getId());
            utilisateurMap.put("firstName", commentaire.getUtilisateur().getFirstName());
            // Autres propriétés utilisateur que vous voulez inclure
            commentaireMap.put("utilisateur", utilisateurMap);

            // Information sur le récepteur sans références circulaires
            Map<String, Object> recepteurMap = new HashMap<>();
            recepteurMap.put("id", commentaire.getRecepteur().getId());
            recepteurMap.put("firstName", commentaire.getRecepteur().getFirstName());
            // Autres propriétés récepteur que vous voulez inclure
            commentaireMap.put("recepteur", recepteurMap);

            // Indique si les images sont disponibles
            commentaireMap.put("hasImageAvant", commentaire.getImageAvant() != null && commentaire.getImageAvant().length > 0);
            commentaireMap.put("hasImageApres", commentaire.getImageApres() != null && commentaire.getImageApres().length > 0);

            // URLs pour accéder aux images
            if (commentaire.getImageAvant() != null && commentaire.getImageAvant().length > 0) {
                commentaireMap.put("imageAvantUrl", "/api/commentaires/" + commentaire.getId() + "/image-avant");
            }

            if (commentaire.getImageApres() != null && commentaire.getImageApres().length > 0) {
                commentaireMap.put("imageApresUrl", "/api/commentaires/" + commentaire.getId() + "/image-apres");
            }

            resultList.add(commentaireMap);
        }

        return ResponseEntity.ok(resultList);
    }

    // Récupérer les commentaires laissés par un gymzer
    @GetMapping("/laisses/{utilisateurId}")
    public ResponseEntity<List<Commentaire>> getCommentairesLaisses(@PathVariable Long utilisateurId) {
        List<Commentaire> commentaires = commentaireService.getCommentairesLaisses(utilisateurId);
        return ResponseEntity.ok(commentaires);
    }

    // Récupérer les commentaires entre deux utilisateurs
    @GetMapping("/entre/{recepteurId}/{utilisateurId}")
    public ResponseEntity<List<Commentaire>> getCommentairesEntreUtilisateurs(
            @PathVariable Long recepteurId, @PathVariable Long utilisateurId) {
        List<Commentaire> commentaires = commentaireService.getCommentairesEntreUtilisateurs(recepteurId, utilisateurId);
        return ResponseEntity.ok(commentaires);
    }

    // Récupérer un commentaire par son ID
    @GetMapping("/{id}")
    public ResponseEntity<Commentaire> getCommentaireParId(@PathVariable Long id) {
        Optional<Commentaire> commentaire = commentaireService.getCommentaireParId(id);
        return commentaire.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    // Récupérer l'image avant d'un commentaire
    @GetMapping("/{id}/image-avant")
    public ResponseEntity<byte[]> getImageAvant(@PathVariable Long id) {
        Optional<Commentaire> commentaire = commentaireService.getCommentaireParId(id);
        if (commentaire.isPresent() && commentaire.get().getImageAvant() != null) {
            return ResponseEntity.ok()
                    .contentType(MediaType.IMAGE_JPEG)
                    .body(commentaire.get().getImageAvant());
        }
        return ResponseEntity.notFound().build();
    }

    // Récupérer l'image après d'un commentaire
    @GetMapping("/{id}/image-apres")
    public ResponseEntity<byte[]> getImageApres(@PathVariable Long id) {
        Optional<Commentaire> commentaire = commentaireService.getCommentaireParId(id);
        if (commentaire.isPresent() && commentaire.get().getImageApres() != null) {
            return ResponseEntity.ok()
                    .contentType(MediaType.IMAGE_JPEG)
                    .body(commentaire.get().getImageApres());
        }
        return ResponseEntity.notFound().build();
    }

    // Supprimer un commentaire
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> supprimerCommentaire(@PathVariable Long id) {
        commentaireService.supprimerCommentaire(id);
        return ResponseEntity.noContent().build();
    }

    // Mettre à jour un commentaire
    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> mettreAJourCommentaire(
            @PathVariable Long id,
            @RequestParam(value = "evaluation", required = false) Integer evaluation,
            @RequestParam(value = "commentaire", required = false) String commentaire,
            @RequestParam(value = "imageAvant", required = false) MultipartFile imageAvant,
            @RequestParam(value = "imageApres", required = false) MultipartFile imageApres) {

        try {
            Optional<Commentaire> existingCommentaire = commentaireService.getCommentaireParId(id);
            if (existingCommentaire.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            Commentaire commentaireToUpdate = existingCommentaire.get();

            // Mise à jour des champs si fournis
            if (evaluation != null) {
                commentaireToUpdate.setEvaluation(evaluation);
            }

            if (commentaire != null && !commentaire.isBlank()) {
                commentaireToUpdate.setCommentaire(commentaire);
            }

            // Mise à jour des images si fournies
            if (imageAvant != null && !imageAvant.isEmpty()) {
                commentaireToUpdate.setImageAvant(imageAvant.getBytes());
            }

            if (imageApres != null && !imageApres.isEmpty()) {
                commentaireToUpdate.setImageApres(imageApres.getBytes());
            }

            Commentaire updatedCommentaire = commentaireService.mettreAJourCommentaire(id, commentaireToUpdate);
            return ResponseEntity.ok(updatedCommentaire);

        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erreur lors du traitement des images: " + e.getMessage());
        }
    }
}