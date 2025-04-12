package com.example.demo.controller;

import com.example.demo.model.Course;
import com.example.demo.model.Enrollment;
import com.example.demo.model.User;
import com.example.demo.service.CourseService;
import com.example.demo.service.PaymentService;
import com.example.demo.service.UserService;
import com.stripe.exception.StripeException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/payments")
@CrossOrigin(origins = "*")
public class PaymentController {

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private CourseService courseService;

    @Autowired
    private UserService userService;

    @PostMapping("/create-payment")
    public ResponseEntity<?> createPayment(
            @RequestParam("courseId") Long courseId,
            @RequestParam("userId") Long userId) {
        try {
            Course course = courseService.getCourseById(courseId);
            User user = userService.getUserById(userId)
                    .orElseThrow(() -> new IllegalArgumentException("Utilisateur non trouvé"));

            Map<String, Object> paymentData = paymentService.createPaymentIntent(course, user);
            return ResponseEntity.ok(paymentData);
        } catch (StripeException e) {
            return ResponseEntity.badRequest().body("Erreur lors de la création du paiement: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Erreur: " + e.getMessage());
        }
    }

    @PostMapping("/webhook")
    public ResponseEntity<?> handleStripeWebhook(@RequestBody String payload, @RequestHeader("Stripe-Signature") String sigHeader) {
        try {
            // Cette méthode est plus complexe et nécessiterait une implémentation spécifique
            // pour vérifier la signature et traiter les événements Stripe
            // Voir la documentation Stripe pour plus de détails
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Erreur: " + e.getMessage());
        }
    }

    @PostMapping("/confirm")
    public ResponseEntity<?> confirmPayment(
            @RequestParam("paymentIntentId") String paymentIntentId,
            @RequestParam("status") String status) {
        try {
            Enrollment enrollment = paymentService.confirmPayment(paymentIntentId, status);
            return ResponseEntity.ok(enrollment);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Erreur: " + e.getMessage());
        }
    }
}