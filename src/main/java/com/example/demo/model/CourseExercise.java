package com.example.demo.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "course_exercise")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class CourseExercise {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(length = 1000)
    private String description;

    @Column(name = "duration_seconds", nullable = false)
    private Integer durationSeconds;

    @Column(name = "repetitions")
    private String repetitions; // ex: "2 x 10"

    @Column(nullable = false)
    private Integer orderIndex;

    @Column(name = "is_locked", nullable = false)
    private boolean isLocked = true;

    // Si c'est le premier exercice, il peut être gratuit
    @Column(name = "is_free_preview", nullable = false)
    private boolean isFreePreview = false;

    // Relation avec le cours
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id")
    @JsonBackReference
    private Course course;

    // Relation avec la vidéo de l'exercice
    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "video_id")
    private ExerciseVideo video;
}