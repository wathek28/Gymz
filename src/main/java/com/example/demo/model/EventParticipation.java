package com.example.demo.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Table(name = "event_participation")
public class EventParticipation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @Column(name = "participation_date")
    private LocalDateTime participationDate;

    @Column(name = "email")
    private String email;
    @Column(name = "first_name", nullable = false)
    private String firstName;
    @Column(name = "phone_number", nullable = false)
    private String phoneNumber;


    @PrePersist
    protected void onCreate() {
        participationDate = LocalDateTime.now();
        if (user != null) {
            email = user.getEmail();
            firstName = user.getFirstName();
            phoneNumber = user.getPhoneNumber();
        }
    }
}