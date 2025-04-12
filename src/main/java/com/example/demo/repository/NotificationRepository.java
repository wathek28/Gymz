package com.example.demo.repository;

import com.example.demo.model.Notification;
import com.example.demo.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByDestinataireOrderByDateCreationDesc(User destinataire);
    List<Notification> findByDestinataireAndLueOrderByDateCreationDesc(User destinataire, boolean lue);
    long countByDestinataireAndLue(User destinataire, boolean lue);
}