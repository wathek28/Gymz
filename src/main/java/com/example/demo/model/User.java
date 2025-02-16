package com.example.demo.model;

import com.example.demo.model.Role;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;


@Setter
@Getter
@Entity
@Table(name = "user")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Column(unique = true, nullable = false )
    private String phoneNumber;

    @Column(nullable = false)
    private boolean verified = false;

    private String verificationCode;

    @Column(length = 100)
    @Size(min = 2, max = 100)
    private String firstName;

    @Column(unique = true)
    @Email
    @Size(max = 255)
    private String email;

    @Lob
    @Column(name = "photo", columnDefinition = "LONGBLOB", nullable = true)
    private byte[] photo;
    @Column(name = "lastname")
    private String nom;
    private String bio;
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
}
