package com.example.demo.controller;

import com.example.demo.service.ContactService;
import com.example.demo.service.ContactService.ContactMessageDTO;
import com.example.demo.service.ContactService.ContactMessageResponseDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/contact")
@CrossOrigin(origins = "*") // À modifier selon les besoins de sécurité
@RequiredArgsConstructor
public class ContactController {

    private final ContactService contactService;

    /**
     * Crée un nouveau message de contact avec les IDs du coach et du gymzer dans les en-têtes
     * Peut être utilisé par un gymzer connecté ou un visiteur
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> createContactMessage(
            @RequestHeader(value = "X-Coach-Id", required = false) Long coachId,
            @RequestHeader(value = "X-Gymzer-Id", required = false) Long gymzerId,
            @Valid @RequestBody ContactMessageDTO contactMessageDTO) {

        // Définir les IDs à partir des en-têtes s'ils sont présents
        if (coachId != null) {
            contactMessageDTO.setCoachId(coachId);
        }

        if (gymzerId != null) {
            contactMessageDTO.setGymzerId(gymzerId);
        }

        ContactMessageResponseDTO savedMessage = contactService.createContactMessage(contactMessageDTO);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of(
                        "message", "Message envoyé avec succès",
                        "id", savedMessage.getId(),
                        "data", savedMessage
                ));
    }

    /**
     * Envoie un message direct entre un coach et un gymzer
     */
    @PostMapping("/coach/{coachId}/gymzer/{gymzerId}")
    public ResponseEntity<Map<String, Object>> sendMessageBetweenCoachAndGymzer(
            @PathVariable Long coachId,
            @PathVariable Long gymzerId,
            @Valid @RequestBody ContactMessageDTO contactMessageDTO) {
        // Définir explicitement les IDs du coach et du gymzer
        contactMessageDTO.setCoachId(coachId);
        contactMessageDTO.setGymzerId(gymzerId);

        ContactMessageResponseDTO savedMessage = contactService.createContactMessage(contactMessageDTO);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of(
                        "message", "Message envoyé avec succès",
                        "id", savedMessage.getId(),
                        "data", savedMessage
                ));
    }

    /**
     * Récupère tous les messages d'un coach avec pagination
     */
    @GetMapping("/coach/{coachId}")
    public ResponseEntity<Page<ContactMessageResponseDTO>> getCoachMessages(
            @PathVariable Long coachId,
            @PageableDefault(size = 20, sort = "dateEnvoi", direction = org.springframework.data.domain.Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(contactService.getMessagesByCoach(coachId, pageable));
    }

    /**
     * Récupère tous les messages entre un coach et un gymzer spécifique
     */
    @GetMapping("/coach/{coachId}/gymzer/{gymzerId}")
    public ResponseEntity<List<ContactMessageResponseDTO>> getMessagesBetweenCoachAndGymzer(
            @PathVariable Long coachId,
            @PathVariable Long gymzerId) {
        return ResponseEntity.ok(contactService.getMessagesByCoachAndGymzer(coachId, gymzerId));
    }

    /**
     * Récupère tous les messages envoyés par un gymzer
     */
    @GetMapping("/gymzer/{gymzerId}")
    public ResponseEntity<List<ContactMessageResponseDTO>> getGymzerMessages(@PathVariable Long gymzerId) {
        return ResponseEntity.ok(contactService.getMessagesByGymzer(gymzerId));
    }

    /**
     * Récupère un message spécifique
     */
    @GetMapping("/{messageId}")
    public ResponseEntity<ContactMessageResponseDTO> getMessageById(@PathVariable Long messageId) {
        return ResponseEntity.ok(contactService.getMessageById(messageId));
    }

    /**
     * Marque un message comme lu
     */
    @PatchMapping("/{messageId}/read")
    public ResponseEntity<Map<String, Object>> markAsRead(@PathVariable Long messageId) {
        ContactMessageResponseDTO updatedMessage = contactService.markMessageAsRead(messageId);

        return ResponseEntity.ok(Map.of(
                "message", "Message marqué comme lu avec succès",
                "data", updatedMessage
        ));
    }

    /**
     * Compte les messages non lus pour un coach
     */
    @GetMapping("/coach/{coachId}/unread-count")
    public ResponseEntity<Map<String, Object>> countUnreadMessages(@PathVariable Long coachId) {
        Long count = contactService.countUnreadMessages(coachId);

        return ResponseEntity.ok(Map.of(
                "count", count,
                "coachId", coachId
        ));
    }

    /**
     * Récupère les messages non lus pour un coach
     */


    /**
     * Supprime un message
     */


    /**
     * Gestionnaire d'exceptions global pour le contrôleur
     * Gère les exceptions spécifiques et génériques
     */
    @ExceptionHandler({
            org.springframework.web.server.ResponseStatusException.class,
            jakarta.validation.ValidationException.class,
            IllegalArgumentException.class,
            Exception.class
    })
    public ResponseEntity<Map<String, Object>> handleExceptions(Exception e) {
        Map<String, Object> errorResponse = new HashMap<>();
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;

        if (e instanceof org.springframework.web.server.ResponseStatusException rsEx) {
            status = rsEx.getStatusCode().is4xxClientError()
                    ? HttpStatus.BAD_REQUEST
                    : HttpStatus.INTERNAL_SERVER_ERROR;
            errorResponse.put("error", rsEx.getReason());
        } else if (e instanceof jakarta.validation.ValidationException) {
            status = HttpStatus.BAD_REQUEST;
            errorResponse.put("error", "Erreur de validation : " + e.getMessage());
        } else if (e instanceof IllegalArgumentException) {
            status = HttpStatus.BAD_REQUEST;
            errorResponse.put("error", "Requête invalide : " + e.getMessage());
        } else {
            errorResponse.put("error", "Une erreur inattendue est survenue");
        }

        // Ajout d'informations de débogage supplémentaires
        errorResponse.put("timestamp", java.time.LocalDateTime.now());
        errorResponse.put("status", status.value());

        return ResponseEntity.status(status).body(errorResponse);
    }
}