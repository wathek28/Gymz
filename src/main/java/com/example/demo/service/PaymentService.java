package com.example.demo.service;

import com.example.demo.model.Course;
import com.example.demo.model.Enrollment;
import com.example.demo.model.PaymentStatus;
import com.example.demo.model.User;
import com.example.demo.repository.EnrollmentRepository;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentCreateParams;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Service
public class PaymentService {

    @Autowired
    private EnrollmentRepository enrollmentRepository;

    @Transactional
    public Map<String, Object> createPaymentIntent(Course course, User user) throws StripeException {
        // Vérifier si l'utilisateur est déjà inscrit à ce cours
        boolean alreadyEnrolled = enrollmentRepository.existsByCourseAndUser(course, user);
        if (alreadyEnrolled) {
            throw new IllegalStateException("Vous êtes déjà inscrit à ce cours");
        }

        // Convertir le prix en centimes (Stripe utilise les plus petites unités monétaires)
        long amount = course.getPrice().multiply(new BigDecimal("100")).longValue();

        // Créer les paramètres pour l'intention de paiement
        PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                .setCurrency("eur") // ou "usd" ou autre selon votre devise
                .setAmount(amount)
                .setDescription("Inscription au cours: " + course.getTitle())
                .putMetadata("courseId", course.getId().toString())
                .putMetadata("userId", user.getId().toString())
                .setReceiptEmail(user.getEmail())
                .build();

        // Créer l'intention de paiement avec Stripe
        PaymentIntent paymentIntent = PaymentIntent.create(params);

        // Créer une inscription en attente de paiement
        Enrollment enrollment = new Enrollment();
        enrollment.setCourse(course);
        enrollment.setUser(user);
        enrollment.setPaidAmount(course.getPrice());
        enrollment.setPaymentStatus(PaymentStatus.PENDING);
        enrollment.setTransactionId(paymentIntent.getId());
        enrollmentRepository.save(enrollment);

        // Retourner les informations nécessaires au client
        Map<String, Object> response = new HashMap<>();
        response.put("clientSecret", paymentIntent.getClientSecret());
        response.put("paymentIntentId", paymentIntent.getId());
        response.put("amount", amount);
        response.put("currency", "eur");

        return response;
    }

    @Transactional
    public Enrollment confirmPayment(String paymentIntentId, String status) {
        // Trouver l'inscription associée à cette intention de paiement
        Enrollment enrollment = enrollmentRepository.findByTransactionId(paymentIntentId)
                .orElseThrow(() -> new IllegalArgumentException("Aucune inscription trouvée pour ce paiement"));

        // Mettre à jour le statut du paiement
        if ("succeeded".equals(status)) {
            enrollment.setPaymentStatus(PaymentStatus.COMPLETED);
        } else if ("processing".equals(status)) {
            enrollment.setPaymentStatus(PaymentStatus.PROCESSING);
        } else {
            enrollment.setPaymentStatus(PaymentStatus.FAILED);
        }

        return enrollmentRepository.save(enrollment);
    }
}