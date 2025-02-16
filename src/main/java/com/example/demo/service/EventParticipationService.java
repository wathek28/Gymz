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

import java.util.Optional;

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

        return participationRepository.save(participation);
    }

    public Optional<EventParticipation> findParticipation(Long eventId, Long userId) {
        return participationRepository.findByEventIdAndUserId(eventId, userId);
    }
}
