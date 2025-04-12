package com.example.demo.service;

import com.example.demo.model.Event;
import com.example.demo.model.EventParticipation;
import com.example.demo.model.User;
import com.example.demo.repository.EventParticipationRepository;
import com.example.demo.repository.EventRepository;
import com.example.demo.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EventParticipationService {
    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final EventParticipationRepository participationRepository;

    @Transactional
    public EventParticipation participateInEvent(Long eventId, Long userId) {
        // Vérifier si l'utilisateur n'est pas déjà inscrit
        if (participationRepository.existsByUserIdAndEventId(userId, eventId)) {
            throw new RuntimeException("User is already registered for this event");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Event not found"));

        EventParticipation participation = new EventParticipation();
        participation.setUser(user);
        participation.setEvent(event);
        participation.setEmail(user.getEmail()); // Ajout explicite de l'email
        participation.setFirstName(user.getFirstName());
        participation.setPhoneNumber(user.getPhoneNumber());

        return participationRepository.save(participation);
    }

    @Transactional
    public EventParticipation registerForEvent(Long eventId, EventRegistrationRequest request) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Event not found"));

        // Vérifier si l'email est déjà utilisé pour cet événement
        if (participationRepository.existsByEmailAndEventId(request.getEmail(), eventId)) {
            throw new RuntimeException("This email is already registered for this event");
        }

        EventParticipation participation = new EventParticipation();
        participation.setEvent(event);
        participation.setEmail(request.getEmail());
        participation.setFirstName(request.getFirstName());
        participation.setPhoneNumber(request.getPhoneNumber());
        // User est null car c'est une inscription sans compte utilisateur

        return participationRepository.save(participation);
    }

    // Nouvelle méthode pour permettre à un utilisateur existant de remplir un formulaire
    @Transactional
    public EventParticipation registerExistingUserWithForm(Long eventId, Long userId, EventRegistrationRequest request) {
        // Vérifier si l'utilisateur n'est pas déjà inscrit
        if (participationRepository.existsByUserIdAndEventId(userId, eventId)) {
            throw new RuntimeException("User is already registered for this event");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Event not found"));

        EventParticipation participation = new EventParticipation();
        participation.setUser(user);
        participation.setEvent(event);

        // Utiliser les informations du formulaire plutôt que celles de l'utilisateur
        participation.setEmail(request.getEmail());
        participation.setFirstName(request.getFirstName());
        participation.setPhoneNumber(request.getPhoneNumber());

        return participationRepository.save(participation);
    }

    public Optional<EventParticipation> findParticipation(Long eventId, Long userId) {
        return participationRepository.findByEventIdAndUserId(eventId, userId);
    }

    public Optional<EventParticipation> findParticipationByEmail(Long eventId, String email) {
        return participationRepository.findByEventIdAndEmail(eventId, email);
    }
    @Transactional(readOnly = true)
    public List<Event> findEventsByUserParticipation(Long userId) {
        // Retrieve all event participations for the given user
        List<EventParticipation> participations = participationRepository.findByUserId(userId);

        // Extract and return the events from participations
        return participations.stream()
                .map(EventParticipation::getEvent)
                .collect(Collectors.toList());
    }
}