package com.example.demo.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDateTime;
@Data

@AllArgsConstructor

@Setter
@Getter
@Entity
@Table(name = "commentaire")
public class Commentaire {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Column(nullable = false)
    private Integer evaluation;  // Evaluation sous forme d'un entier (par exemple 1-5)

    @NotNull
    @Size(min = 1, max = 500)
    @Column(nullable = false, length = 500)
    private String commentaire;  // Le texte du commentaire

    @NotNull
    @Column(nullable = false)
    private LocalDateTime dateCommentaire;  // La date et l'heure du commentaire

    // Relation avec l'utilisateur qui a écrit le commentaire
    @ManyToOne
    @JoinColumn(name = "utilisateur_id", nullable = false)
    private User utilisateur;  // L'utilisateur qui a écrit le commentaire

    // Relation avec l'utilisateur qui reçoit le commentaire
    @ManyToOne
    @JoinColumn(name = "recepteur_id", nullable = false)
    private User recepteur;  // L'utilisateur qui reçoit le commentaire

    // Images avant et après (elles seront stockées en tant que BLOB)
    @Lob
    @Column(name = "image_avant", columnDefinition = "LONGBLOB")
    private byte[] imageAvant;

    @Lob
    @Column(name = "image_apres", columnDefinition = "LONGBLOB")
    private byte[] imageApres;

    // Constructeur par défaut
    public Commentaire() {
        this.dateCommentaire = LocalDateTime.now();  // Initialisation avec la date et l'heure actuelles
    }

}
