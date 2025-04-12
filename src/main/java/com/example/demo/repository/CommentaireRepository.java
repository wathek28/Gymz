package com.example.demo.repository;

import com.example.demo.model.Commentaire;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommentaireRepository extends JpaRepository<Commentaire, Long> {

    // Trouver les commentaires reçus par un utilisateur
    List<Commentaire> findByRecepteur_Id(Long recepteurId);

    // Trouver les commentaires laissés par un utilisateur
    List<Commentaire> findByUtilisateur_Id(Long utilisateurId);

    // Trouver les commentaires entre deux utilisateurs
    List<Commentaire> findByRecepteur_IdAndUtilisateur_Id(Long recepteurId, Long utilisateurId);
}