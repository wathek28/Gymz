package com.example.demo.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "plannings")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Planning {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String dayName;  // ex: "Lundi", "Mardi", etc.

    @Column(nullable = false)
    private String dayTitle; // ex: "Force & Musculation", "Cardio & Endurance", etc.

    @Column(nullable = false)
    private String activity1; // ex: "Musculation Haut du Corps"

    @Column(nullable = false)
    private String time1;     // ex: "06:00 - 08:00"

    @Column(nullable = false)
    private String activity2; // ex: "Musculation Bas du Corps"

    @Column(nullable = false)
    private String time2;     // ex: "18:00 - 20:00"

    // Relation avec l'utilisateur (gym)
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
}