package com.example.demo.controller;

import com.example.demo.model.EventParticipation;
import com.example.demo.service.EventParticipationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import lombok.RequiredArgsConstructor;

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
}
