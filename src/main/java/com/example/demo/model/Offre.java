package com.example.demo.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "offres")
public class Offre {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String titre;

    @Column(length = 1000)
    private String description;

    @Column(nullable = false)
    private Double prix;

    @ManyToOne
    @JoinColumn(name = "createur_id")
    private User createur;

    @Column(nullable = false)
    private LocalDateTime dateCreation;

    @Enumerated(EnumType.STRING)
    private OffreStatut statut = OffreStatut.ACTIVE;

    @Column(unique = true)
    private String codePromo;

    @Column
    private Double pourcentageReduction = 0.0;

    public Double getPourcentageReduction() {
        return pourcentageReduction;
    }

    public void setPourcentageReduction(Double pourcentageReduction) {
        this.pourcentageReduction = pourcentageReduction != null ? pourcentageReduction : 0.0;
    }

    public Double getPrixApresReduction() {
        double reduction = getPourcentageReduction();
        return prix * (1 - reduction / 100);
    }

    public String getCodePromo() {
        return codePromo;
    }

    public void setCodePromo(String codePromo) {
        this.codePromo = codePromo;
    }

    // Constructeurs
    public Offre() {
        this.dateCreation = LocalDateTime.now();
    }

    // Getters et setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitre() {
        return titre;
    }

    public void setTitre(String titre) {
        this.titre = titre;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Double getPrix() {
        return prix;
    }

    public void setPrix(Double prix) {
        this.prix = prix;
    }

    public User getCreateur() {
        return createur;
    }

    public void setCreateur(User createur) {
        this.createur = createur;
    }

    public LocalDateTime getDateCreation() {
        return dateCreation;
    }

    public OffreStatut getStatut() {
        return statut;
    }

    public void setStatut(OffreStatut statut) {
        this.statut = statut;
    }

    // Enum pour le statut de l'offre
    public enum OffreStatut {
        ACTIVE,
        INACTIVE,
        ARCHIVE
    }
    public class OffreDto {
        private String titre;
        private String description;
        private Double prix;
        private Long gymId;
        // Si l'offre est liée à un gymnase
        // Getters et setters
    }
}