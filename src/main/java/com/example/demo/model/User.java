package com.example.demo.model;

import com.example.demo.model.Role;
import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Setter
@Getter
@Entity
@Table(name = "user")

@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class User {

    /// ////////////////////cours

    // Cours créés par cet utilisateur (s'il est coach)
    @OneToMany(mappedBy = "coach")
    @JsonManagedReference("coach-courses")
    private List<Course> courses = new ArrayList<>();

    @OneToMany(mappedBy = "user")
    @JsonManagedReference("user-enrollments")
    private List<Enrollment> enrollments = new ArrayList<>();



    /// ////////////////////////



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


    @Column(name = "birth_date")
    private LocalDate birthDate;

    @Column(name = "address", length = 255)
    private String address;

    // Liste des commentaires reçus par l'utilisateur
    @OneToMany(mappedBy = "recepteur", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Commentaire> commentairesRecus;

    // Liste des commentaires laissés par l'utilisateur
    @OneToMany(mappedBy = "utilisateur", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Commentaire> commentairesLaisses;


    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonManagedReference
    private List<Abonnement> abonnements = new ArrayList<>();

    @Lob
    @Column(name = "photo", columnDefinition = "LONGBLOB")
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


    /// /photo
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)

    @JsonManagedReference
    private List<Photo> photos = new ArrayList<>();


    /// //// update profil user
    @Column(name = "temp_phone_number")
    private String tempPhoneNumber;

    // Ajoutez ses getters et setters
    public String getTempPhoneNumber() {
        return tempPhoneNumber;
    }

    public void setTempPhoneNumber(String tempPhoneNumber) {
        this.tempPhoneNumber = tempPhoneNumber;
    }
}

