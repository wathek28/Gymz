package com.example.demo.service;

import com.example.demo.model.Offre;
import com.example.demo.model.Role;
import com.example.demo.model.User;
import com.example.demo.repository.OffreRepository;
import com.example.demo.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class OffreService {
    @Autowired
    private OffreRepository offreRepository;

    @Autowired
    private UserService userService;



    @Autowired
    private UserRepository userRepository; // Utiliser directement le repository

    private String generateUniqueCode() {
        return "PROMO" + System.currentTimeMillis() % 10000;
    }



    @Transactional
    public Offre creerOffre(String phoneNumber, OffreDto offreDto) {
        User createur = userRepository.findByPhoneNumber(phoneNumber)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        Offre offre = new Offre();
        offre.setTitre(offreDto.getTitre());
        offre.setDescription(offreDto.getDescription());
        offre.setPrix(offreDto.getPrix());
        offre.setCreateur(createur);
        offre.setCodePromo(generateUniqueCode());
        // Ajout de cette ligne
        offre.setPourcentageReduction(offreDto.getPourcentageReduction());

        return offreRepository.save(offre);
    }

    @Transactional(readOnly = true)
    public List<Offre> getOffresUtilisateur(String phoneNumber) {
        User createur = userRepository.findByPhoneNumber(phoneNumber)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé avec ce numéro de téléphone"));

        return offreRepository.findByCreateur(createur);
    }

    @Transactional(readOnly = true)
    public Optional<Offre> getOffreById(Long id) {
        return offreRepository.findById(id);
    }

    @Transactional
    public Offre modifierOffre(Long id, OffreDto offreDto) {
        Offre offre = offreRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Offre non trouvée"));

        offre.setTitre(offreDto.getTitre());
        offre.setDescription(offreDto.getDescription());
        offre.setPrix(offreDto.getPrix());

        return offreRepository.save(offre);
    }

    @Transactional
    public void supprimerOffre(Long id) {
        Offre offre = offreRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Offre non trouvée"));

        offreRepository.delete(offre);
    }
    // Dans OffreService
    @Transactional(readOnly = true)
    public List<Offre> getAllOffres() {
        return offreRepository.findAll();
    }







    // DTO à déplacer dans un package séparé
    public static class OffreDto {
        private String titre;
        private String description;
        private Double prix;
        private Double pourcentageReduction = 0.0;

        // Constructeur par défaut
        public OffreDto() {}

        // Constructeur avec paramètres
        public OffreDto(String titre, String description, Double prix, Double pourcentageReduction) {
            this.titre = titre;
            this.description = description;
            this.prix = prix;
            this.pourcentageReduction = pourcentageReduction != null ? pourcentageReduction : 0.0;
        }

        // Getters et Setters
        public String getTitre() { return titre; }
        public void setTitre(String titre) { this.titre = titre; }

        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }

        public Double getPrix() { return prix; }
        public void setPrix(Double prix) { this.prix = prix; }

        public Double getPourcentageReduction() { return pourcentageReduction; }
        public void setPourcentageReduction(Double pourcentageReduction) {
            this.pourcentageReduction = pourcentageReduction != null ? pourcentageReduction : 0.0;
        }
    }
}