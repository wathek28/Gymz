package com.example.demo.repository;

import com.example.demo.model.ExerciseVideo;
import com.example.demo.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ExerciseVideoRepository extends JpaRepository<ExerciseVideo, Long> {

    List<ExerciseVideo> findByCoachId(Long coachId);

    // Recherche des vidéos preview
    List<ExerciseVideo> findByPreviewTrue();

    // Recherche des vidéos par titre (insensible à la casse)
    List<ExerciseVideo> findByTitleContainingIgnoreCase(String title);

    // Compter le nombre de vidéos par coach
    long countByCoachId(Long coachId);  // Changé de countByCoach(User coach)

    // Recherche des vidéos créées après une date donnée
    List<ExerciseVideo> findByCreatedAtAfter(LocalDateTime date);
}