package com.example.demo.repository;

import com.example.demo.model.Course;
import com.example.demo.model.Enrollment;
import com.example.demo.model.PaymentStatus;
import com.example.demo.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {

    // Trouver toutes les inscriptions d'un utilisateur
    List<Enrollment> findByUser(User user);

    // Trouver toutes les inscriptions pour un cours
    List<Enrollment> findByCourse(Course course);

    // Vérifier si un utilisateur est inscrit à un cours
    Optional<Enrollment> findByUserAndCourse(User user, Course course);

    // Trouver les inscriptions par statut de paiement
    List<Enrollment> findByPaymentStatus(PaymentStatus paymentStatus);

    // Trouver les cours complétés par un utilisateur
    List<Enrollment> findByUserAndIsCompletedTrue(User user);

    // Compter le nombre d'inscriptions pour un cours
    long countByCourse(Course course);
    boolean existsByCourseAndUser(Course course, User user);
    Optional<Enrollment> findByTransactionId(String transactionId);

    // Dans EnrollmentRepository

    List<Enrollment> findByUser_Id(Long userId);
    List<Enrollment> findByUser_IdAndPaymentStatus(Long userId, String paymentStatus);
}