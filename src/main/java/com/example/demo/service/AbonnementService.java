package com.example.demo.service;

import com.example.demo.model.Abonnement;
import com.example.demo.model.Role;
import com.example.demo.model.User;
import com.example.demo.repository.AbonnementRepository;
import com.example.demo.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AbonnementService {

    private final AbonnementRepository abonnementRepository;
    private final UserRepository userRepository;

    @Autowired
    public AbonnementService(AbonnementRepository abonnementRepository, UserRepository userRepository) {
        this.abonnementRepository = abonnementRepository;
        this.userRepository = userRepository;
    }

    /**
     * Crée un nouvel abonnement pour un utilisateur de type gym
     */
    @Transactional
    public Abonnement.Response createAbonnement(Long userId, Abonnement.Request request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Utilisateur non trouvé avec l'ID : " + userId));

        // Vérifier que l'utilisateur est bien un gym
        if (user.getRole() != Role.GYM) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Seuls les utilisateurs de type GYM peuvent créer des abonnements");
        }

        Abonnement abonnement = new Abonnement();
        abonnement.setTitre(request.getTitre());
        abonnement.setDescription(request.getDescription());
        abonnement.setPrix(request.getPrix());
        abonnement.setAncienPrix(request.getAncienPrix());
        abonnement.setDuree(request.getDuree());
        abonnement.setUniteDuree(request.getUniteDuree());
        abonnement.setDateCreation(LocalDateTime.now());
        abonnement.setActif(true);
        abonnement.setUser(user);

        abonnement = abonnementRepository.save(abonnement);
        return new Abonnement.Response(abonnement);
    }

    /**
     * Récupère tous les abonnements d'un gym
     */
    public List<Abonnement.Response> getAbonnementsByUserId(Long userId) {
        List<Abonnement> abonnements = abonnementRepository.findByUserId(userId);
        return abonnements.stream()
                .map(Abonnement.Response::new)
                .collect(Collectors.toList());
    }

    /**
     * Récupère tous les abonnements actifs d'un gym
     */
    public List<Abonnement.Response> getActiveAbonnementsByUserId(Long userId) {
        List<Abonnement> abonnements = abonnementRepository.findByUserIdAndActifTrue(userId);
        return abonnements.stream()
                .map(Abonnement.Response::new)
                .collect(Collectors.toList());
    }

    /**
     * Récupère un abonnement par son ID
     */
    public Abonnement.Response getAbonnementById(Long id) {
        Abonnement abonnement = abonnementRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Abonnement non trouvé avec l'ID : " + id));
        return new Abonnement.Response(abonnement);
    }

    /**
     * Met à jour un abonnement existant
     */
    @Transactional
    public Abonnement.Response updateAbonnement(Long id, Abonnement.Request request) {
        Abonnement abonnement = abonnementRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Abonnement non trouvé avec l'ID : " + id));

        abonnement.setTitre(request.getTitre());
        abonnement.setDescription(request.getDescription());
        abonnement.setPrix(request.getPrix());
        abonnement.setAncienPrix(request.getAncienPrix());
        abonnement.setDuree(request.getDuree());
        abonnement.setUniteDuree(request.getUniteDuree());
        abonnement.setDateModification(LocalDateTime.now());

        abonnement = abonnementRepository.save(abonnement);
        return new Abonnement.Response(abonnement);
    }

    /**
     * Change le statut actif/inactif d'un abonnement
     */
    @Transactional
    public Abonnement.Response changeAbonnementStatus(Long id, boolean actif) {
        Abonnement abonnement = abonnementRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Abonnement non trouvé avec l'ID : " + id));

        abonnement.setActif(actif);
        abonnement.setDateModification(LocalDateTime.now());

        abonnement = abonnementRepository.save(abonnement);
        return new Abonnement.Response(abonnement);
    }

    /**
     * Supprime un abonnement
     */
    @Transactional
    public void deleteAbonnement(Long id) {
        if (!abonnementRepository.existsById(id)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Abonnement non trouvé avec l'ID : " + id);
        }
        abonnementRepository.deleteById(id);
    }
}