package com.example.demo.service;

import com.example.demo.model.Planning;
import com.example.demo.model.Role;
import com.example.demo.model.User;
import com.example.demo.repository.PlanningRepository;
import com.example.demo.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class PlanningService {

    @Autowired
    private PlanningRepository planningRepository;

    @Autowired
    private UserRepository userRepository;

    /**
     * Récupère tous les plannings d'un gym
     */
    public List<Planning> getPlanningsByUserId(Long userId) {
        return planningRepository.findByUserIdOrderByDayNameAsc(userId);
    }

    /**
     * Récupère un planning par son ID
     */
    public Optional<Planning> getPlanningById(Long id) {
        return planningRepository.findById(id);
    }

    /**
     * Crée un nouveau planning
     */
    @Transactional
    public Planning createPlanning(Planning planning, Long userId) {
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isPresent()) {
            User user = userOpt.get();

            // Vérifier que l'utilisateur est un gym
            if (user.getRole() != Role.GYM) {
                throw new IllegalArgumentException("Seuls les utilisateurs avec le rôle GYM peuvent créer des plannings");
            }

            planning.setUser(user);
            return planningRepository.save(planning);
        } else {
            throw new IllegalArgumentException("Utilisateur non trouvé avec l'ID: " + userId);
        }
    }

    /**
     * Met à jour un planning existant
     */
    @Transactional
    public Planning updatePlanning(Planning planningDetails, Long id) {
        Optional<Planning> planningOpt = planningRepository.findById(id);
        if (planningOpt.isPresent()) {
            Planning planning = planningOpt.get();

            // Mettre à jour les champs
            planning.setTitle(planningDetails.getTitle());
            planning.setDayName(planningDetails.getDayName());
            planning.setDayTitle(planningDetails.getDayTitle());
            planning.setActivity1(planningDetails.getActivity1());
            planning.setTime1(planningDetails.getTime1());
            planning.setActivity2(planningDetails.getActivity2());
            planning.setTime2(planningDetails.getTime2());

            return planningRepository.save(planning);
        } else {
            throw new IllegalArgumentException("Planning non trouvé avec l'ID: " + id);
        }
    }

    /**
     * Supprime un planning
     */
    @Transactional
    public void deletePlanning(Long id) {
        Optional<Planning> planningOpt = planningRepository.findById(id);
        if (planningOpt.isPresent()) {
            planningRepository.deleteById(id);
        } else {
            throw new IllegalArgumentException("Planning non trouvé avec l'ID: " + id);
        }
    }
}