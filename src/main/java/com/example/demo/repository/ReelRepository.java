package com.example.demo.repository;


import com.example.demo.model.Reel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;


public interface ReelRepository extends JpaRepository<Reel, Long> {
    List<Reel> findByUserId(Long userId); // Assurez-vous que cette méthode est présente
}
