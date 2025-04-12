package com.example.demo.model;

import com.example.demo.model.CourseExercise;
import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "exercise_video")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class ExerciseVideo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    private String description;

    @Column(name = "video_path", nullable = false)
    private String videoPath;

    @Column(name = "original_filename")
    private String originalFilename;

    @Column(name = "duration_seconds")
    private Integer durationSeconds;

    @Column(name = "is_preview", nullable = false)
    private boolean preview = false;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    // Relation avec l'exercice rendue optionnelle
    @OneToOne(mappedBy = "video", fetch = FetchType.LAZY)
    @JsonBackReference
    private CourseExercise exercise;

    // Suppression de la relation avec le coach
    // À la place, stockage uniquement de l'ID du coach
    @Column(name = "coach_id", nullable = false)
    private Long coachId;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}