package com.example.demo.repository;

import com.example.demo.model.Offre;
import com.example.demo.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OffreRepository extends JpaRepository<Offre, Long> {
    // Modification : utilisez 'createur' au lieu de 'userId'
    List<Offre> findByCreateur(User createur);
    Optional<Offre> findById(Long id);
}