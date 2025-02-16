package com.example.demo.repository;

import com.example.demo.model.EventParticipation;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface EventParticipationRepository extends JpaRepository<EventParticipation, Long> {
    boolean existsByUserIdAndEventId(Long userId, Long eventId);
    Optional<EventParticipation> findByEventIdAndUserId(Long eventId, Long userId);
}
