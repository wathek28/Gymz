package com.example.demo.repository;

import com.example.demo.model.Course;
import com.example.demo.model.CourseExercise;
import com.example.demo.model.CourseLevel;
import com.example.demo.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CourseRepository extends JpaRepository<Course, Long> {

    // Trouver tous les cours par niveau
    List<Course> findByLevel(CourseLevel level);

    // Trouver tous les cours d'un coach
    List<Course> findByCoach(User coach);

    // Trouver tous les cours gratuits
    List<Course> findByIsPaidFalse();

    // Trouver tous les cours payants
    List<Course> findByIsPaidTrue();

    // Trouver les cours les plus récents
    List<Course> findTop10ByOrderByCreatedAtDesc();

    // Recherche par titre ou description
    @Query("SELECT c FROM Course c WHERE LOWER(c.title) LIKE LOWER(CONCAT('%', ?1, '%')) " +
            "OR LOWER(c.description) LIKE LOWER(CONCAT('%', ?1, '%'))")
    List<Course> search(String keyword);


    // Cette requête retourne uniquement les cours qui ont des inscriptions pour l'utilisateur


    // Variante avec filtre par statut de paiement si nécessaire
    @Query(value = "SELECT DISTINCT c.* FROM course c " +
            "JOIN enrollment e ON c.id = e.course_id " +
            "WHERE e.user_id = :userId " +
            "AND e.payment_status IN :statuses",
            nativeQuery = true)
    List<Course> findPurchasedCoursesByUserIdAndStatuses(
            @Param("userId") Long userId,
            @Param("statuses") List<String> statuses
    );
    @Query(value = "SELECT DISTINCT c.* FROM course c " +
            "INNER JOIN enrollment e ON c.id = e.course_id " +
            "WHERE e.user_id = :userId",
            nativeQuery = true)
    List<Course> findPurchasedCoursesByUserId(@Param("userId") Long userId);

}