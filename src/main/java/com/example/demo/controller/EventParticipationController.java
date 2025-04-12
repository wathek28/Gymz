package com.example.demo.controller;

import com.example.demo.model.Event;
import com.example.demo.model.EventParticipation;
import com.example.demo.service.EventParticipationService;
import com.example.demo.service.EventRegistrationRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
public class EventParticipationController {
    private final EventParticipationService participationService;

    @PostMapping("/{eventId}/participate/{userId}")
    public ResponseEntity<?> participateInEvent(
            @PathVariable Long eventId,
            @PathVariable Long userId) {

        // Vérifier si l'utilisateur est déjà inscrit
        Optional<EventParticipation> existingParticipation = participationService.findParticipation(eventId, userId);

        if (existingParticipation.isPresent()) {
            return ResponseEntity.badRequest().body("Vous êtes déjà inscrit à cet événement !");
        }

        // Ajouter la participation
        EventParticipation participation = participationService.participateInEvent(eventId, userId);
        return ResponseEntity.ok(participation);
    }

    @PostMapping("/{eventId}/register")
    public ResponseEntity<?> registerForEvent(
            @PathVariable Long eventId,
            @RequestBody EventRegistrationRequest registrationRequest) {

        // Vérifier si l'email est déjà utilisé pour cet événement
        Optional<EventParticipation> existingParticipation =
                participationService.findParticipationByEmail(eventId, registrationRequest.getEmail());

        if (existingParticipation.isPresent()) {
            return ResponseEntity.badRequest().body("Cette adresse email est déjà inscrite à cet événement !");
        }

        try {
            // Enregistrer la participation
            EventParticipation participation = participationService.registerForEvent(eventId, registrationRequest);
            return ResponseEntity.ok(participation);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Erreur lors de l'inscription: " + e.getMessage());
        }
    }

    // Nouvel endpoint pour permettre à un utilisateur spécifique de s'inscrire via formulaire
    @PostMapping("/{eventId}/participate/{userId}/form")
    public ResponseEntity<?> registerExistingUserWithForm(
            @PathVariable Long eventId,
            @PathVariable Long userId,
            @RequestBody EventRegistrationRequest registrationRequest) {

        // Vérifier si l'utilisateur est déjà inscrit
        Optional<EventParticipation> existingParticipation = participationService.findParticipation(eventId, userId);

        if (existingParticipation.isPresent()) {
            return ResponseEntity.badRequest().body("Vous êtes déjà inscrit à cet événement !");
        }

        try {
            // Enregistrer la participation avec les données du formulaire
            EventParticipation participation = participationService.registerExistingUserWithForm(
                    eventId, userId, registrationRequest);
            return ResponseEntity.ok(participation);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Erreur lors de l'inscription: " + e.getMessage());
        }
    }
    @GetMapping("/user/{userId}/events")
    public ResponseEntity<List<Event>> getUserEvents(@PathVariable Long userId) {
        List<Event> userEvents = participationService.findEventsByUserParticipation(userId);

        if (userEvents.isEmpty()) {
            return ResponseEntity.noContent().build();
        }

        return ResponseEntity.ok(userEvents);
    }
}