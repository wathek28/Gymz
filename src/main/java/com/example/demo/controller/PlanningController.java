package com.example.demo.controller;

import com.example.demo.model.Planning;
import com.example.demo.service.PlanningService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@RestController
@RequestMapping("/api/plannings")
@CrossOrigin(origins = "*")
public class PlanningController {

    // Classe DTO intégrée directement dans le contrôleur
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PlanningDTO {
        private Long id;
        private String title;
        private String dayName;  // ex: "Lundi", "Mardi", etc.
        private String dayTitle; // ex: "Force & Musculation", "Cardio & Endurance", etc.
        private String activity1; // ex: "Musculation Haut du Corps"
        private String time1;     // ex: "06:00 - 08:00"
        private String activity2; // ex: "Musculation Bas du Corps"
        private String time2;     // ex: "18:00 - 20:00"
        private Long userId;     // ID de l'utilisateur associé
    }

    @Autowired
    private PlanningService planningService;

    /**
     * Récupère tous les plannings d'un gym
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<PlanningDTO>> getPlanningsByUserId(@PathVariable Long userId) {
        try {
            List<Planning> plannings = planningService.getPlanningsByUserId(userId);

            // Convertir en DTO pour éviter les références circulaires
            List<PlanningDTO> planningDTOs = plannings.stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());

            return new ResponseEntity<>(planningDTOs, HttpStatus.OK);
        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Récupère un planning par son ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<PlanningDTO> getPlanningById(@PathVariable Long id) {
        try {
            Optional<Planning> planning = planningService.getPlanningById(id);
            if (planning.isPresent()) {
                PlanningDTO dto = convertToDTO(planning.get());
                return new ResponseEntity<>(dto, HttpStatus.OK);
            } else {
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Crée un nouveau planning
     */
    @PostMapping("/user/{userId}")
    public ResponseEntity<PlanningDTO> createPlanning(@RequestBody PlanningDTO planningDTO, @PathVariable Long userId) {
        try {
            // Convertir DTO en entité
            Planning planning = convertToEntity(planningDTO);

            Planning createdPlanning = planningService.createPlanning(planning, userId);

            PlanningDTO createdDTO = convertToDTO(createdPlanning);
            return new ResponseEntity<>(createdDTO, HttpStatus.CREATED);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Met à jour un planning existant
     */
    @PutMapping("/{id}")
    public ResponseEntity<PlanningDTO> updatePlanning(@RequestBody PlanningDTO planningDTO, @PathVariable Long id) {
        try {
            // Convertir DTO en entité
            Planning planning = convertToEntity(planningDTO);

            Planning updatedPlanning = planningService.updatePlanning(planning, id);

            PlanningDTO updatedDTO = convertToDTO(updatedPlanning);
            return new ResponseEntity<>(updatedDTO, HttpStatus.OK);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Supprime un planning
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePlanning(@PathVariable Long id) {
        try {
            planningService.deletePlanning(id);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Convertit une entité Planning en PlanningDTO
     */
    private PlanningDTO convertToDTO(Planning planning) {
        PlanningDTO dto = new PlanningDTO();
        dto.setId(planning.getId());
        dto.setTitle(planning.getTitle());
        dto.setDayName(planning.getDayName());
        dto.setDayTitle(planning.getDayTitle());
        dto.setActivity1(planning.getActivity1());
        dto.setTime1(planning.getTime1());
        dto.setActivity2(planning.getActivity2());
        dto.setTime2(planning.getTime2());

        // Ne pas inclure l'objet User complet, seulement l'ID
        if (planning.getUser() != null) {
            dto.setUserId(planning.getUser().getId());
        }

        return dto;
    }

    /**
     * Convertit un PlanningDTO en entité Planning
     * Note: l'utilisateur sera défini par le service
     */
    private Planning convertToEntity(PlanningDTO dto) {
        Planning planning = new Planning();
        planning.setId(dto.getId());
        planning.setTitle(dto.getTitle());
        planning.setDayName(dto.getDayName());
        planning.setDayTitle(dto.getDayTitle());
        planning.setActivity1(dto.getActivity1());
        planning.setTime1(dto.getTime1());
        planning.setActivity2(dto.getActivity2());
        planning.setTime2(dto.getTime2());

        // L'utilisateur sera défini par le service en utilisant l'ID
        return planning;
    }
}