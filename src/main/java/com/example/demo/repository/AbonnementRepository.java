package com.example.demo.repository;

import com.example.demo.model.Abonnement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AbonnementRepository extends JpaRepository<Abonnement, Long> {

    // Trouver tous les abonnements par user_id
    List<Abonnement> findByUserId(Long userId);

    // Trouver les abonnements actifs par user_id
    List<Abonnement> findByUserIdAndActifTrue(Long userId);

    // Trouver par titre (recherche partielle insensible à la casse)
    List<Abonnement> findByTitreContainingIgnoreCase(String titre);

    // Trouver par plage de prix
    List<Abonnement> findByPrixBetween(Double prixMin, Double prixMax);

    // Trouver par unité de durée
    List<Abonnement> findByUniteDuree(Abonnement.UniteDuree uniteDuree);
}