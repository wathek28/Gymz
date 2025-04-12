package com.example.demo.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
public class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String titre;

    @Column(length = 1000)
    private String message;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnoreProperties({"notifications", "password", "events", "offres"})
    private User destinataire;

    @Column(nullable = false)
    private LocalDateTime dateCreation;

    @Column(nullable = false)
    private boolean lue = false;

    @Enumerated(EnumType.STRING)
    private NotificationType type;

    @Column
    private Long referenceId; // ID de l'événement ou de l'offre associée

    public enum NotificationType {
        NOUVELLE_OFFRE,
        OFFRE_MODIFIEE,
        NOUVEL_EVENEMENT,
        EVENEMENT_MODIFIE,
        EVENEMENT_RAPPEL,
        SYSTEME
    }

    // Constructeur par défaut
    public Notification() {
        this.dateCreation = LocalDateTime.now();
    }

    // Constructeur avec paramètres
    public Notification(String titre, String message, User destinataire, NotificationType type, Long referenceId) {
        this.titre = titre;
        this.message = message;
        this.destinataire = destinataire;
        this.type = type;
        this.referenceId = referenceId;
        this.dateCreation = LocalDateTime.now();
    }

    // Getters et Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitre() { return titre; }
    public void setTitre(String titre) { this.titre = titre; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public User getDestinataire() { return destinataire; }
    public void setDestinataire(User destinataire) { this.destinataire = destinataire; }

    public LocalDateTime getDateCreation() { return dateCreation; }
    public void setDateCreation(LocalDateTime dateCreation) { this.dateCreation = dateCreation; }

    public boolean isLue() { return lue; }
    public void setLue(boolean lue) { this.lue = lue; }

    public NotificationType getType() { return type; }
    public void setType(NotificationType type) { this.type = type; }

    public Long getReferenceId() { return referenceId; }
    public void setReferenceId(Long referenceId) { this.referenceId = referenceId; }
}