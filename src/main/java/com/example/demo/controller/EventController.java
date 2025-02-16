package com.example.demo.controller;

import com.example.demo.model.Event;
import com.example.demo.service.EventService;
import com.example.demo.service.JwtTokenService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@RestController
@RequestMapping("/api/events")
public class EventController {
    private static final Logger logger = LoggerFactory.getLogger(EventController.class);

    @Autowired
    private EventService eventService;

    @Autowired
    private JwtTokenService jwtTokenService;

    @PostMapping("/creer")
    public ResponseEntity<?> creerEvent(
            @RequestParam("titre") String titre,
            @RequestParam("date") String date,
            @RequestParam("adresse") String adresse,
            @RequestParam("prix") Double prix,
            @RequestParam("heureDebut") String heureDebut,
            @RequestParam("heureFin") String heureFin,
            @RequestParam("description") String description,
            @RequestParam("reglement") String reglement,
            @RequestParam(value = "photo", required = false) MultipartFile photo, // Handle photo upload
            HttpServletRequest request
    ) {
        try {
            String phoneNumber = extractPhoneNumberFromToken(request);

            // Construct the EventDto manually using the request parameters
            EventService.EventDto eventDto = new EventService.EventDto();
            eventDto.setTitre(titre);
            eventDto.setDate(LocalDate.parse(date)); // Assuming date is in the correct format
            eventDto.setAdresse(adresse);
            eventDto.setPrix(BigDecimal.valueOf(prix));
            eventDto.setHeureDebut(LocalTime.parse(heureDebut)); // Assuming time is in the correct format
            eventDto.setHeureFin(LocalTime.parse(heureFin)); // Assuming time is in the correct format
            eventDto.setDescription(description);
            eventDto.setReglement(reglement);

            // If a photo is provided, convert it to a byte array and set it on the DTO
            if (photo != null && !photo.isEmpty()) {
                byte[] photoBytes = photo.getBytes();
                eventDto.setPhoto(photoBytes);
            }

            Event event = eventService.createEvent(phoneNumber, eventDto);
            return ResponseEntity.ok(event);
        } catch (IllegalArgumentException e) {
            logger.warn("Erreur de validation: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ErrorResponse(e.getMessage()));
        } catch (IOException e) {
            logger.error("Erreur lors de la conversion de l'image", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Erreur lors du téléchargement de la photo"));
        } catch (Exception e) {
            logger.error("Erreur création événement", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Une erreur s'est produite."));
        }
    }

    private String extractPhoneNumberFromToken(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new IllegalArgumentException("Token d'authentification manquant ou invalide");
        }

        String token = authHeader.substring(7);
        return jwtTokenService.extractPhoneNumber(token);
    }

    private void validateEventDto(EventService.EventDto eventDto) {
        if (eventDto.getTitre() == null || eventDto.getTitre().trim().isEmpty()) {
            throw new IllegalArgumentException("Le titre de l'événement est obligatoire");
        }
        if (eventDto.getPrix() == null || eventDto.getPrix().doubleValue() <= 0) {
            throw new IllegalArgumentException("Le prix doit être un nombre positif");
        }
    }

    @GetMapping
    public ResponseEntity<?> getAllEvents() {
        try {
            List<Event> events = eventService.getAllEvents();
            return ResponseEntity.ok(events);
        } catch (Exception e) {
            logger.error("Erreur récupération des événements", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Erreur lors de la récupération des événements"));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getEventById(@PathVariable Long id) {
        try {
            Event event = eventService.getEventById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Événement non trouvé"));
            return ResponseEntity.ok(event);
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            logger.error("Erreur récupération événement", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Erreur lors de la récupération de l'événement"));
        }
    }

    @GetMapping("/mes-events")
    public ResponseEntity<?> getEventsUtilisateur(HttpServletRequest request) {
        try {
            String phoneNumber = extractPhoneNumberFromToken(request);
            List<Event> events = eventService.getEventsUtilisateur(phoneNumber);
            return ResponseEntity.ok(events);
        } catch (Exception e) {
            logger.error("Erreur récupération événements utilisateur", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Erreur lors de la récupération des événements"));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> supprimerEvent(@PathVariable Long id, HttpServletRequest request) {
        try {
            String phoneNumber = extractPhoneNumberFromToken(request);
            Event event = eventService.getEventById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Événement non trouvé"));

            if (!event.getCreateur().getPhoneNumber().equals(phoneNumber)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(new ErrorResponse("Vous n'êtes pas autorisé à supprimer cet événement"));
            }

            eventService.supprimerEvent(id, phoneNumber);
            return ResponseEntity.ok().build();
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Erreur lors de la suppression"));
        }
    }

    public static class ErrorResponse {
        private String message;

        public ErrorResponse(String message) {
            this.message = message;
        }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
    }

    // Custom exception for resource not found
    public static class ResourceNotFoundException extends RuntimeException {
        public ResourceNotFoundException(String message) {
            super(message);
        }
    }
}
