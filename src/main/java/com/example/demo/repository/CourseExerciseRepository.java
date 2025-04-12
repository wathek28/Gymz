package com.example.demo.repository;

import com.example.demo.model.Course;
import com.example.demo.model.CourseExercise;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CourseExerciseRepository extends JpaRepository<CourseExercise, Long> {

    // Trouver tous les exercices d'un cours
    List<CourseExercise> findByCourseOrderByOrderIndexAsc(Course course);

    // Trouver les exercices gratuits d'un cours
    List<CourseExercise> findByCourseAndIsFreePreviewTrue(Course course);

    // Trouver les exercices verrouillés d'un cours
    List<CourseExercise> findByCourseAndIsLockedTrue(Course course);

    // Trouver les exercices déverrouillés d'un cours
    List<CourseExercise> findByCourseAndIsLockedFalse(Course course);

    // Supprimer tous les exercices d'un cours
    void deleteByCourse(Course course);

    // Trouver les exercices d'un cours par l'ID du cours
    List<CourseExercise> findByCourseIdOrderByOrderIndex(Long courseId);
}