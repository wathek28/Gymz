package com.example.demo.repository;

import com.example.demo.model.ContactMessage;
import com.example.demo.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ContactMessageRepository extends JpaRepository<ContactMessage, Long> {
    List<ContactMessage> findByCoachOrderByDateEnvoiDesc(User coach);
    List<ContactMessage> findByCoachAndLuOrderByDateEnvoiDesc(User coach, boolean lu);
    Long countByCoachAndLu(User coach, boolean lu);
    Page<ContactMessage> findByCoach(User coach, Pageable pageable);

    // Méthodes pour les Gymzers
    List<ContactMessage> findByGymzerOrderByDateEnvoiDesc(User gymzer);
    Long countByGymzer(User gymzer);

    /**
     * Récupère les messages entre un coach et un gymzer spécifique
     * @param coach Le coach
     * @param gymzer Le gymzer
     * @return Liste des messages entre le coach et le gymzer
     */
    @Query("SELECT cm FROM ContactMessage cm WHERE " +
            "(cm.coach = :coach AND cm.gymzer = :gymzer) OR " +
            "(cm.coach = :gymzer AND cm.gymzer = :coach) " +
            "ORDER BY cm.dateEnvoi DESC")
    List<ContactMessage> findMessagesBetweenCoachAndGymzer(
            @Param("coach") User coach,
            @Param("gymzer") User gymzer
    );
}