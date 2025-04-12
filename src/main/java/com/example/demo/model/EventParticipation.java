package com.example.demo.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
@Data
@NoArgsConstructor
@AllArgsConstructor

@Entity
@Getter
@Setter
@Table(name = "event_participation")
public class EventParticipation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = true)  // Modifié pour être nullable
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @Column(name = "participation_date")
    private LocalDateTime participationDate;

    @Column(name = "email", nullable = false)
    private String email;

    @Column(name = "first_name", nullable = false)
    private String firstName;

    @Column(name = "phone_number", nullable = false)
    private String phoneNumber;

    @PrePersist
    protected void onCreate() {
        participationDate = LocalDateTime.now();
        if (user != null) {
            // Si l'utilisateur est défini, on récupère ses infos
            if (email == null) {
                email = user.getEmail();
            }
            if (firstName == null) {
                firstName = user.getFirstName();
            }
            if (phoneNumber == null) {
                phoneNumber = user.getPhoneNumber();
            }
        }
    }
}