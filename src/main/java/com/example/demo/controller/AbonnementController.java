package com.example.demo.controller;

import com.example.demo.model.Abonnement;
import com.example.demo.service.AbonnementService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/abonnements")
@CrossOrigin(origins = "*")
public class AbonnementController {

    private final AbonnementService abonnementService;

    @Autowired
    public AbonnementController(AbonnementService abonnementService) {
        this.abonnementService = abonnementService;
    }

    /**
     * Crée un nouvel abonnement pour un gym
     */
    @PostMapping("/user/{userId}")
    public ResponseEntity<Abonnement.Response> createAbonnement(
            @PathVariable Long userId,
            @Valid @RequestBody Abonnement.Request request) {
        try {
            Abonnement.Response response = abonnementService.createAbonnement(userId, request);
            return new ResponseEntity<>(response, HttpStatus.CREATED);
        } catch (ResponseStatusException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Erreur lors de la création de l'abonnement: " + ex.getMessage()
            );
        }
    }

    /**
     * Récupère tous les abonnements d'un gym
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Abonnement.Response>> getAbonnementsByUserId(
            @PathVariable Long userId) {
        try {
            List<Abonnement.Response> abonnements = abonnementService.getAbonnementsByUserId(userId);
            return ResponseEntity.ok(abonnements);
        } catch (Exception ex) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Erreur lors de la récupération des abonnements: " + ex.getMessage()
            );
        }
    }

    /**
     * Récupère tous les abonnements actifs d'un gym
     */
    @GetMapping("/user/{userId}/actifs")
    public ResponseEntity<List<Abonnement.Response>> getActiveAbonnementsByUserId(
            @PathVariable Long userId) {
        try {
            List<Abonnement.Response> abonnements = abonnementService.getActiveAbonnementsByUserId(userId);
            return ResponseEntity.ok(abonnements);
        } catch (Exception ex) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Erreur lors de la récupération des abonnements actifs: " + ex.getMessage()
            );
        }
    }

    /**
     * Récupère un abonnement par son ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<Abonnement.Response> getAbonnementById(
            @PathVariable Long id) {
        try {
            Abonnement.Response abonnement = abonnementService.getAbonnementById(id);
            return ResponseEntity.ok(abonnement);
        } catch (ResponseStatusException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Erreur lors de la récupération de l'abonnement: " + ex.getMessage()
            );
        }
    }

    /**
     * Met à jour un abonnement existant
     */
    @PutMapping("/{id}")
    public ResponseEntity<Abonnement.Response> updateAbonnement(
            @PathVariable Long id,
            @Valid @RequestBody Abonnement.Request request) {
        try {
            Abonnement.Response response = abonnementService.updateAbonnement(id, request);
            return ResponseEntity.ok(response);
        } catch (ResponseStatusException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Erreur lors de la mise à jour de l'abonnement: " + ex.getMessage()
            );
        }
    }

    /**
     * Change le statut actif/inactif d'un abonnement
     */
    @PatchMapping("/{id}/status")
    public ResponseEntity<Abonnement.Response> changeAbonnementStatus(
            @PathVariable Long id,
            @RequestParam boolean actif) {
        try {
            Abonnement.Response response = abonnementService.changeAbonnementStatus(id, actif);
            return ResponseEntity.ok(response);
        } catch (ResponseStatusException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Erreur lors du changement de statut de l'abonnement: " + ex.getMessage()
            );
        }
    }

    /**
     * Supprime un abonnement
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAbonnement(
            @PathVariable Long id) {
        try {
            abonnementService.deleteAbonnement(id);
            return ResponseEntity.noContent().build();
        } catch (ResponseStatusException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Erreur lors de la suppression de l'abonnement: " + ex.getMessage()
            );
        }
    }
}