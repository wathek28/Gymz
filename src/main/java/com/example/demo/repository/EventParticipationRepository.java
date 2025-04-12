package com.example.demo.repository;

import com.example.demo.model.EventParticipation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EventParticipationRepository extends JpaRepository<EventParticipation, Long> {
    boolean existsByUserIdAndEventId(Long userId, Long eventId);

    boolean existsByEmailAndEventId(String email, Long eventId);

    Optional<EventParticipation> findByEventIdAndUserId(Long eventId, Long userId);

    Optional<EventParticipation> findByEventIdAndEmail(Long eventId, String email);
    List<EventParticipation> findByUserId(Long userId);
}