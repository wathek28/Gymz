package com.example.demo.service;

import com.example.demo.model.Event;
import com.example.demo.model.Role;
import com.example.demo.model.User;
import com.example.demo.repository.EventRepository;
import com.example.demo.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
public class EventService {
    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Transactional
    public Event createEvent(String phoneNumber, EventDto eventDto) {
        User createur = userRepository.findByPhoneNumber(phoneNumber)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        // Vérification du rôle
        if (createur.getRole() != Role.COACH && createur.getRole() != Role.GYM) {
            throw new RuntimeException("Seuls les coachs et les gyms peuvent créer des événements");
        }

        Event event = new Event();
        event.setTitre(eventDto.getTitre());
        event.setDate(eventDto.getDate());
        event.setAdresse(eventDto.getAdresse());
        event.setPrix(eventDto.getPrix());
        event.setHeureDebut(eventDto.getHeureDebut());
        event.setHeureFin(eventDto.getHeureFin());
        event.setDescription(eventDto.getDescription());
        event.setReglement(eventDto.getReglement());
        event.setCreateur(createur);

        // Ajouter la photo si présente
        if (eventDto.getPhoto() != null) {
            event.setPhoto(eventDto.getPhoto());
        }

        return eventRepository.save(event);
    }

    @Transactional(readOnly = true)
    public List<Event> getEventsUtilisateur(String phoneNumber) {
        User createur = userRepository.findByPhoneNumber(phoneNumber)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        return eventRepository.findByCreateur(createur);
    }

    @Transactional(readOnly = true)
    public Optional<Event> getEventById(Long id) {
        return eventRepository.findById(id);
    }

    @Transactional
    public Event modifierEvent(Long id, String phoneNumber, EventDto eventDto) {
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Événement non trouvé"));

        // Vérifier que l'utilisateur est le créateur
        User user = userRepository.findByPhoneNumber(phoneNumber)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        if (!event.getCreateur().getId().equals(user.getId())) {
            throw new RuntimeException("Vous n'êtes pas autorisé à modifier cet événement");
        }

        event.setTitre(eventDto.getTitre());
        event.setDate(eventDto.getDate());
        event.setAdresse(eventDto.getAdresse());
        event.setPrix(eventDto.getPrix());
        event.setHeureDebut(eventDto.getHeureDebut());
        event.setHeureFin(eventDto.getHeureFin());
        event.setDescription(eventDto.getDescription());
        event.setReglement(eventDto.getReglement());

        // Modifier la photo si présente
        if (eventDto.getPhoto() != null) {
            event.setPhoto(eventDto.getPhoto());
        }

        return eventRepository.save(event);
    }

    @Transactional
    public void supprimerEvent(Long id, String phoneNumber) {
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Événement non trouvé"));

        User user = userRepository.findByPhoneNumber(phoneNumber)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        if (!event.getCreateur().getId().equals(user.getId())) {
            throw new RuntimeException("Vous n'êtes pas autorisé à supprimer cet événement");
        }

        eventRepository.delete(event);
    }

    @Transactional(readOnly = true)
    public List<Event> getAllEvents() {
        return eventRepository.findAll();
    }

    // DTO intégré dans le service comme pour OffreService
    public static class EventDto {
        private String titre;
        private LocalDate date;
        private String adresse;
        private BigDecimal prix;
        private LocalTime heureDebut;
        private LocalTime heureFin;
        private String description;
        private String reglement;
        private byte[] photo;  // Ajout du champ photo

        // Constructeur par défaut
        public EventDto() {}

        // Constructeur avec paramètres
        public EventDto(String titre, LocalDate date, String adresse, BigDecimal prix,
                        LocalTime heureDebut, LocalTime heureFin, String description, String reglement, byte[] photo) {
            this.titre = titre;
            this.date = date;
            this.adresse = adresse;
            this.prix = prix;
            this.heureDebut = heureDebut;
            this.heureFin = heureFin;
            this.description = description;
            this.reglement = reglement;
            this.photo = photo;
        }

        // Getters et Setters
        public String getTitre() { return titre; }
        public void setTitre(String titre) { this.titre = titre; }

        public LocalDate getDate() { return date; }
        public void setDate(LocalDate date) { this.date = date; }

        public String getAdresse() { return adresse; }
        public void setAdresse(String adresse) { this.adresse = adresse; }

        public BigDecimal getPrix() { return prix; }
        public void setPrix(BigDecimal prix) { this.prix = prix; }

        public LocalTime getHeureDebut() { return heureDebut; }
        public void setHeureDebut(LocalTime heureDebut) { this.heureDebut = heureDebut; }

        public LocalTime getHeureFin() { return heureFin; }
        public void setHeureFin(LocalTime heureFin) { this.heureFin = heureFin; }

        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }

        public String getReglement() { return reglement; }
        public void setReglement(String reglement) { this.reglement = reglement; }

        public byte[] getPhoto() { return photo; }
        public void setPhoto(byte[] photo) { this.photo = photo; }
    }
}
