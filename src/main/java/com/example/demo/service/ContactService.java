package com.example.demo.service;

import com.example.demo.model.ContactMessage;
import com.example.demo.model.Role;
import com.example.demo.model.User;
import com.example.demo.repository.ContactMessageRepository;
import com.example.demo.repository.UserRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Validated
public class ContactService {

    @Autowired
    private ContactMessageRepository contactMessageRepository;

    @Autowired
    private UserRepository userRepository;

    /**
     * Vérifie si une chaîne est vide ou ne contient que des espaces
     */
    private boolean isBlank(String str) {
        return str == null || str.trim().isEmpty();
    }

    /**
     * DTO pour la création d'un message de contact
     */
    @Getter
    @Setter
    public static class ContactMessageDTO {
        private String nom = "";
        private String email = "";
        private String telephone = "";

        @NotBlank(message = "La raison de contact est obligatoire")
        private String raisonContact;

        @NotBlank(message = "Le message est obligatoire")
        @Size(min = 10, max = 1000, message = "Le message doit contenir entre 10 et 1000 caractères")
        private String message;

        private Long coachId;
        private Long gymzerId;
    }

    /**
     * DTO pour la réponse d'un message de contact
     */
    @Getter
    @Setter
    public static class ContactMessageResponseDTO {
        private Long id;
        private String nom;
        private String email;
        private String telephone;
        private String raisonContact;
        private String message;
        private LocalDateTime dateEnvoi;
        private Long coachId;
        private String coachNom;
        private String coachEmail;
        private Long gymzerId;
        private String gymzerNom;
        private String gymzerEmail;
        private boolean lu;

        /**
         * Convertit une entité ContactMessage en DTO
         */
        public static ContactMessageResponseDTO fromEntity(ContactMessage contactMessage) {
            ContactMessageResponseDTO dto = new ContactMessageResponseDTO();
            dto.setId(contactMessage.getId());
            dto.setNom(Optional.ofNullable(contactMessage.getNom()).orElse(""));
            dto.setEmail(Optional.ofNullable(contactMessage.getEmail()).orElse(""));
            dto.setTelephone(Optional.ofNullable(contactMessage.getTelephone()).orElse(""));
            dto.setRaisonContact(contactMessage.getRaisonContact());
            dto.setMessage(contactMessage.getMessage());
            dto.setDateEnvoi(contactMessage.getDateEnvoi());
            dto.setLu(contactMessage.isLu());

            if (contactMessage.getCoach() != null) {
                dto.setCoachId(contactMessage.getCoach().getId());
                dto.setCoachNom(Optional.ofNullable(contactMessage.getCoach().getNom()).orElse(""));
                dto.setCoachEmail(Optional.ofNullable(contactMessage.getCoach().getEmail()).orElse(""));
            }

            if (contactMessage.getGymzer() != null) {
                dto.setGymzerId(contactMessage.getGymzer().getId());
                dto.setGymzerNom(Optional.ofNullable(contactMessage.getGymzer().getNom()).orElse(""));
                dto.setGymzerEmail(Optional.ofNullable(contactMessage.getGymzer().getEmail()).orElse(""));
            }

            return dto;
        }
    }

    /**
     * Valide manuellement les données du message de contact
     */
    private void validateContactMessageData(ContactMessageDTO dto) {
        // Vérifier que les champs obligatoires sont présents
        if (isBlank(dto.getRaisonContact())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "La raison de contact est obligatoire");
        }

        if (isBlank(dto.getMessage())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Le message est obligatoire");
        }

        if (dto.getMessage().length() < 10 || dto.getMessage().length() > 1000) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Le message doit contenir entre 10 et 1000 caractères");
        }

        if (dto.getCoachId() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "L'ID du coach est obligatoire");
        }

        // Validation pour les visiteurs (non gymzer)
        if (dto.getGymzerId() == null || dto.getGymzerId() <= 0) {
            // Validation du nom
            if (isBlank(dto.getNom())) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "Le nom est obligatoire pour les visiteurs");
            }

            // Validation de l'email
            if (isBlank(dto.getEmail())) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "L'email est obligatoire pour les visiteurs");
            }

            // Validation du téléphone
            if (isBlank(dto.getTelephone())) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "Le téléphone est obligatoire pour les visiteurs");
            }

            // Validation du format email
            if (!dto.getEmail().matches("^[\\w-\\.]+@([\\w-]+\\.)+[\\w-]{2,4}$")) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "Veuillez fournir une adresse email valide");
            }
        }
    }

    /**
     * Crée un nouveau message de contact
     */
    @Transactional
    public ContactMessageResponseDTO createContactMessage(@Valid ContactMessageDTO contactMessageDTO) {
        // Validation manuelle des données
        validateContactMessageData(contactMessageDTO);

        // Récupération du coach destinataire
        User coach = userRepository.findById(contactMessageDTO.getCoachId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Coach non trouvé avec l'ID : " + contactMessageDTO.getCoachId()));

        // Vérification que le destinataire est bien un coach
        if (coach.getRole() != Role.COACH) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Le destinataire doit être un coach");
        }

        // Création et configuration du message
        ContactMessage contactMessage = new ContactMessage();
        contactMessage.setRaisonContact(contactMessageDTO.getRaisonContact());
        contactMessage.setMessage(contactMessageDTO.getMessage());
        contactMessage.setDateEnvoi(LocalDateTime.now());
        contactMessage.setCoach(coach);
        contactMessage.setLu(false);

        // Gestion de l'expéditeur
        if (contactMessageDTO.getGymzerId() != null && contactMessageDTO.getGymzerId() > 0) {
            User gymzer = userRepository.findById(contactMessageDTO.getGymzerId())
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.NOT_FOUND, "Utilisateur non trouvé avec l'ID : " + contactMessageDTO.getGymzerId()));

            // Vérification que l'expéditeur est bien un GYMZER
            if (gymzer.getRole() != Role.GYMZER) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "L'expéditeur doit être un GYMZER");
            }

            contactMessage.setGymzer(gymzer);

            // Utiliser les informations du gymzer ou du DTO
            contactMessage.setNom(
                    isBlank(contactMessageDTO.getNom())
                            ? (gymzer.getFirstName() + " " + gymzer.getNom()).trim()
                            : contactMessageDTO.getNom()
            );

            contactMessage.setEmail(
                    isBlank(contactMessageDTO.getEmail())
                            ? gymzer.getEmail()
                            : contactMessageDTO.getEmail()
            );

            contactMessage.setTelephone(
                    isBlank(contactMessageDTO.getTelephone())
                            ? gymzer.getPhoneNumber()
                            : contactMessageDTO.getTelephone()
            );
        } else {
            // Si pas de gymzer, utiliser les informations du DTO
            contactMessage.setNom(contactMessageDTO.getNom());
            contactMessage.setEmail(contactMessageDTO.getEmail());
            contactMessage.setTelephone(contactMessageDTO.getTelephone());
            contactMessage.setGymzer(null);
        }

        // Sauvegarde en base de données
        ContactMessage savedMessage = contactMessageRepository.save(contactMessage);

        // Conversion en DTO pour la réponse
        return ContactMessageResponseDTO.fromEntity(savedMessage);
    }

    /**
     * Récupère tous les messages d'un coach
     */
    public Page<ContactMessageResponseDTO> getMessagesByCoach(Long coachId, Pageable pageable) {
        User coach = userRepository.findById(coachId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Coach non trouvé avec l'ID : " + coachId));

        if (coach.getRole() != Role.COACH) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "L'utilisateur doit être un coach");
        }

        Page<ContactMessage> messagesPage = contactMessageRepository.findByCoach(coach, pageable);

        List<ContactMessageResponseDTO> messagesDTO = messagesPage.getContent().stream()
                .map(ContactMessageResponseDTO::fromEntity)
                .collect(Collectors.toList());

        return new PageImpl<>(messagesDTO, pageable, messagesPage.getTotalElements());
    }

    /**
     * Récupère tous les messages entre un coach et un gymzer
     */
    public List<ContactMessageResponseDTO> getMessagesByCoachAndGymzer(Long coachId, Long gymzerId) {
        User coach = userRepository.findById(coachId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Coach non trouvé avec l'ID : " + coachId));

        User gymzer = userRepository.findById(gymzerId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Gymzer non trouvé avec l'ID : " + gymzerId));

        // Vérification des rôles
        if (coach.getRole() != Role.COACH || gymzer.getRole() != Role.GYMZER) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Utilisateurs invalides");
        }

        List<ContactMessage> messages = contactMessageRepository
                .findMessagesBetweenCoachAndGymzer(coach, gymzer);

        return messages.stream()
                .map(ContactMessageResponseDTO::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Récupère tous les messages d'un gymzer
     */
    public List<ContactMessageResponseDTO> getMessagesByGymzer(Long gymzerId) {
        User gymzer = userRepository.findById(gymzerId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Gymzer non trouvé avec l'ID : " + gymzerId));

        if (gymzer.getRole() != Role.GYMZER) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "L'utilisateur doit être un gymzer");
        }

        List<ContactMessage> messages = contactMessageRepository.findByGymzerOrderByDateEnvoiDesc(gymzer);

        return messages.stream()
                .map(ContactMessageResponseDTO::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Récupère un message par son ID
     */
    public ContactMessageResponseDTO getMessageById(Long messageId) {
        ContactMessage message = contactMessageRepository.findById(messageId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Message non trouvé avec l'ID : " + messageId));

        return ContactMessageResponseDTO.fromEntity(message);
    }

    /**
     * Marque un message comme lu
     */
    @Transactional
    public ContactMessageResponseDTO markMessageAsRead(Long messageId) {
        ContactMessage message = contactMessageRepository.findById(messageId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Message non trouvé avec l'ID : " + messageId));

        message.setLu(true);
        ContactMessage updatedMessage = contactMessageRepository.save(message);

        return ContactMessageResponseDTO.fromEntity(updatedMessage);
    }

    /**
     * Compte les messages non lus pour un coach
     */
    public Long countUnreadMessages(Long coachId) {
        User coach = userRepository.findById(coachId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Coach non trouvé avec l'ID : " + coachId));

        if (coach.getRole() != Role.COACH) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "L'utilisateur doit être un coach");
        }

        return contactMessageRepository.countByCoachAndLu(coach, false);
    }
}
    /**
     * Récupère les messages non lus d'un coach
     */
