package com.example.demo.service;

import com.example.demo.model.*;
import com.example.demo.repository.EventRepository;
import com.example.demo.repository.NotificationRepository;
import com.example.demo.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class NotificationService {

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EventRepository eventRepository;

    @Transactional
    public void notifierNouvelleOffre(Offre offre) {
        System.out.println("Notification pour nouvelle offre: " + offre.getId());
        List<User> utilisateurs = userRepository.findByRole(Role.GYMZER);
        System.out.println("Nombre d'utilisateurs GYMZER: " + utilisateurs.size());


        for (User user : utilisateurs) {
            Notification notification = new Notification(
                    "Nouvelle offre disponible",
                    "Une nouvelle offre \"" + offre.getTitre() + "\" est disponible à " +
                            offre.getPrix() + "€. Code promo: " + offre.getCodePromo(),
                    user,
                    Notification.NotificationType.NOUVELLE_OFFRE,
                    offre.getId()
            );
            notificationRepository.save(notification);
        }
    }

    @Transactional
    public void notifierNouvelEvenement(Event event) {
        List<User> utilisateurs = userRepository.findByRole(Role.GYMZER);

        for (User user : utilisateurs) {
            Notification notification = new Notification(
                    "Nouvel événement à venir",
                    "Un nouvel événement \"" + event.getTitre() + "\" aura lieu le " +
                            event.getDate() + " à " + event.getAdresse() + ". Prix: " + event.getPrix() + "€",
                    user,
                    Notification.NotificationType.NOUVEL_EVENEMENT,
                    event.getId()
            );
            notificationRepository.save(notification);
        }
    }

    @Transactional(readOnly = true)
    public List<Notification> getNotificationsUtilisateur(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        return notificationRepository.findByDestinataireOrderByDateCreationDesc(user);
    }

    @Transactional(readOnly = true)
    public List<Notification> getNotificationsNonLues(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        return notificationRepository.findByDestinataireAndLueOrderByDateCreationDesc(user, false);
    }

    @Transactional
    public void marquerCommeLue(Long notificationId, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Notification non trouvée"));

        if (!notification.getDestinataire().getId().equals(user.getId())) {
            throw new RuntimeException("Vous n'êtes pas autorisé à modifier cette notification");
        }

        notification.setLue(true);
        notificationRepository.save(notification);
    }

    @Transactional
    public void marquerToutesCommeLues(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        List<Notification> notifications = notificationRepository.findByDestinataireAndLueOrderByDateCreationDesc(user, false);
        for (Notification notification : notifications) {
            notification.setLue(true);
            notificationRepository.save(notification);
        }
    }

    @Transactional(readOnly = true)
    public long compterNotificationsNonLues(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        return notificationRepository.countByDestinataireAndLue(user, false);
    }

    @Transactional
    public void notifierRappelEvenement() {
        // Récupérer la date de demain
        LocalDate demain = LocalDate.now().plusDays(1);

        // Récupérer tous les événements qui auront lieu demain
        List<Event> evenementsDemain = eventRepository.findByDate(demain);

        if (evenementsDemain.isEmpty()) {
            return; // Aucun événement demain, rien à faire
        }

        // Récupérer tous les utilisateurs clients
        List<User> clients = userRepository.findByRole(Role.GYMZER);

        // Pour chaque événement de demain, envoyer une notification à tous les clients
        for (Event event : evenementsDemain) {
            String titre = "Rappel: événement demain";
            String message = "Rappel: L'événement \"" + event.getTitre() + "\" aura lieu demain " +
                    event.getDate() + " à " + event.getHeureDebut() + ", " +
                    event.getAdresse() + ". Prix: " + event.getPrix() + "€";

            for (User client : clients) {
                Notification notification = new Notification(
                        titre,
                        message,
                        client,
                        Notification.NotificationType.EVENEMENT_RAPPEL,
                        event.getId()
                );
                notificationRepository.save(notification);
            }
        }

        // Optionnel: Ajouter une entrée de log pour le suivi
        System.out.println("Envoi de rappels pour " + evenementsDemain.size() +
                " événements prévus pour demain (" + demain + ")");
    }
}