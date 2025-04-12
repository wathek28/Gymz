package com.example.demo.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


import java.time.LocalDateTime;
@Data

@AllArgsConstructor
@Setter
@Getter
@Entity
@Table(name = "abonnement")
public class Abonnement {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Le titre de l'abonnement est obligatoire")
    @Column(nullable = false)
    private String titre;

    @Column(columnDefinition = "TEXT")
    private String description;

    @NotNull(message = "Le prix est obligatoire")
    @Positive(message = "Le prix doit être positif")
    @Column(nullable = false)
    private Double prix;

    @Positive(message = "L'ancien prix doit être positif")
    private Double ancienPrix;

    @NotNull(message = "La durée est obligatoire")
    @Positive(message = "La durée doit être positive")
    @Column(nullable = false)
    private Integer duree;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UniteDuree uniteDuree;

    // Date de création de l'abonnement
    @Column(name = "date_creation", nullable = false, updatable = false)
    private LocalDateTime dateCreation;

    // Date de dernière modification
    @Column(name = "date_modification")
    private LocalDateTime dateModification;

    // Statut de l'abonnement (actif, inactif)
    @Column(nullable = false)
    private boolean actif = true;

    // Relation avec l'utilisateur (gym) qui a créé l'abonnement
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonBackReference
    private User user;

    // Constructeur par défaut
    public Abonnement() {
        this.dateCreation = LocalDateTime.now();
    }

    // Enum pour les unités de durée
    public enum UniteDuree {
        JOUR, MOIS, AN
    }

    // Méthodes lifecycle
    @PrePersist
    protected void onCreate() {
        this.dateCreation = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.dateModification = LocalDateTime.now();
    }

    /**
     * Classes intégrées pour les DTOs
     */

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Request {
        @NotBlank(message = "Le titre est obligatoire")
        private String titre;

        private String description;

        @NotNull(message = "Le prix est obligatoire")
        @Positive(message = "Le prix doit être positif")
        private Double prix;

        private Double ancienPrix;

        @NotNull(message = "La durée est obligatoire")
        @Positive(message = "La durée doit être positive")
        private Integer duree;

        @NotNull(message = "L'unité de durée est obligatoire")
        private UniteDuree uniteDuree;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Response {
        private Long id;
        private String titre;
        private String description;
        private Double prix;
        private Double ancienPrix;
        private Integer duree;
        private UniteDuree uniteDuree;
        private LocalDateTime dateCreation;
        private LocalDateTime dateModification;
        private boolean actif;
        private Long userId;

        // Constructeur pour convertir de l'entité au DTO
        public Response(Abonnement abonnement) {
            this.id = abonnement.getId();
            this.titre = abonnement.getTitre();
            this.description = abonnement.getDescription();
            this.prix = abonnement.getPrix();
            this.ancienPrix = abonnement.getAncienPrix();
            this.duree = abonnement.getDuree();
            this.uniteDuree = abonnement.getUniteDuree();
            this.dateCreation = abonnement.getDateCreation();
            this.dateModification = abonnement.getDateModification();
            this.actif = abonnement.isActif();
            this.userId = abonnement.getUser().getId();
        }
    }
}