package com.example.demo.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDateTime;
@Data
@NoArgsConstructor
@AllArgsConstructor

@Setter
@Getter
@Entity
@Table(name = "contact_message")
public class ContactMessage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    @Column(nullable = false)
    private String nom;

    @Email(message = "Veuillez fournir une adresse email valide")
    @Column(nullable = false)
    private String email;

    @Column(nullable = false)
    private String telephone;

    @NotBlank(message = "La raison de contact est obligatoire")
    @Column(nullable = false)
    private String raisonContact;

    @NotBlank(message = "Le message est obligatoire")
    @Size(min = 10, max = 1000, message = "Le message doit contenir entre 10 et 1000 caractères")
    @Column(nullable = false, length = 1000)
    private String message;

    @Column(nullable = false)
    private LocalDateTime dateEnvoi = LocalDateTime.now();

    @ManyToOne
    @JoinColumn(name = "coach_id", nullable = false)
    private User coach;

    @ManyToOne
    @JoinColumn(name = "gymzer_id")
    private User gymzer;

    @Column(nullable = false)
    private boolean lu = false;
}