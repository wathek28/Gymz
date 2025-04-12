package com.example.demo.controller;

import com.example.demo.model.Notification;
import com.example.demo.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    @Autowired
    private NotificationService notificationService;

    @GetMapping
    public ResponseEntity<?> getNotificationsUtilisateur(@RequestParam Long userId) {
        try {
            List<Notification> notifications = notificationService.getNotificationsUtilisateur(userId);
            return ResponseEntity.ok(notifications);
        } catch (RuntimeException e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Erreur lors de la récupération des notifications");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
        }
    }

    @GetMapping("/non-lues")
    public ResponseEntity<?> getNotificationsNonLues(@RequestParam Long userId) {
        try {
            List<Notification> notifications = notificationService.getNotificationsNonLues(userId);
            return ResponseEntity.ok(notifications);
        } catch (RuntimeException e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Erreur lors de la récupération des notifications non lues");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
        }
    }

    @GetMapping("/count")
    public ResponseEntity<?> compterNotificationsNonLues(@RequestParam Long userId) {
        try {
            long count = notificationService.compterNotificationsNonLues(userId);
            return ResponseEntity.ok(count);
        } catch (RuntimeException e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Erreur lors du comptage des notifications non lues");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
        }
    }

    @PutMapping("/{id}/lue")
    public ResponseEntity<?> marquerCommeLue(@PathVariable Long id, @RequestParam Long userId) {
        try {
            notificationService.marquerCommeLue(id, userId);
            return ResponseEntity.ok().build();
        } catch (RuntimeException e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Erreur lors du marquage de la notification comme lue");
            errorResponse.put("message", e.getMessage());

            // Distinguer entre "non trouvé" et "non autorisé"
            if (e.getMessage().contains("autorisé")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
            }
        }
    }

    @PutMapping("/lire-toutes")
    public ResponseEntity<?> marquerToutesCommeLues(@RequestParam Long userId) {
        try {
            notificationService.marquerToutesCommeLues(userId);
            return ResponseEntity.ok().build();
        } catch (RuntimeException e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Erreur lors du marquage de toutes les notifications comme lues");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
        }
    }
}