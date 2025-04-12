package com.example.demo.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.math.BigDecimal;
@Data
@NoArgsConstructor
@AllArgsConstructor

@Setter
@Getter
@Entity
@Table(name = "event")
public class Event {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Size(min = 2, max = 100)
    @Column(nullable = false)
    private String titre;

    @NotNull
    @Column(nullable = false)
    private LocalDate date;

    @NotNull
    @Column(nullable = false)
    private String adresse;

    @NotNull
    @Column(nullable = false)
    private BigDecimal prix;

    @NotNull
    @Column(nullable = false)
    private LocalTime heureDebut;

    @NotNull
    @Column(nullable = false)
    private LocalTime heureFin;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(columnDefinition = "TEXT")
    private String reglement;

    // Nouvelle propriété pour la photo, type BLOB
    @Lob
    @Column(name = "photo", columnDefinition = "LONGBLOB")
    private byte[] photo;

    @JsonIgnore  // Relation avec l'utilisateur qui crée l'événement (coach ou gym)
    @ManyToOne
    @JoinColumn(name = "createur_id", nullable = false)
    private User createur;

    // Méthode pour vérifier si l'utilisateur est autorisé à créer un événement
    public boolean isCreateurAutorise() {
        return createur != null &&
                (createur.getRole() == Role.COACH || createur.getRole() == Role.GYM);
    }

    // Constructeurs, getters et setters sont générés par Lombok
}
