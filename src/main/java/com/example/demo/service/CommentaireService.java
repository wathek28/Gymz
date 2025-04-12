package com.example.demo.service;

import com.example.demo.model.Commentaire;
import com.example.demo.repository.CommentaireRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class CommentaireService {

    private final CommentaireRepository commentaireRepository;

    @Autowired
    public CommentaireService(CommentaireRepository commentaireRepository) {
        this.commentaireRepository = commentaireRepository;
    }

    // Ajouter un commentaire
    public Commentaire ajouterCommentaire(Commentaire commentaire) {
        return commentaireRepository.save(commentaire);
    }

    // Récupérer les commentaires reçus par un utilisateur
    public List<Commentaire> getCommentairesRecus(Long recepteurId) {
        return commentaireRepository.findByRecepteur_Id(recepteurId);
    }

    // Récupérer les commentaires laissés par un utilisateur
    public List<Commentaire> getCommentairesLaisses(Long utilisateurId) {
        return commentaireRepository.findByUtilisateur_Id(utilisateurId);
    }

    // Récupérer les commentaires entre deux utilisateurs
    public List<Commentaire> getCommentairesEntreUtilisateurs(Long recepteurId, Long utilisateurId) {
        return commentaireRepository.findByRecepteur_IdAndUtilisateur_Id(recepteurId, utilisateurId);
    }

    // Récupérer un commentaire par son ID
    public Optional<Commentaire> getCommentaireParId(Long id) {
        return commentaireRepository.findById(id);
    }

    // Supprimer un commentaire
    public void supprimerCommentaire(Long id) {
        commentaireRepository.deleteById(id);
    }

    // Mettre à jour un commentaire
    public Commentaire mettreAJourCommentaire(Long id, Commentaire commentaireMisAJour) {
        if (commentaireRepository.existsById(id)) {
            commentaireMisAJour.setId(id);
            return commentaireRepository.save(commentaireMisAJour);
        }
        return null;
    }
}