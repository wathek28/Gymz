package com.example.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.example.demo.model.Photo;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PhotoRepository extends JpaRepository<Photo, Long> {
    @Query("SELECT p FROM Photo p WHERE p.user.id = :userId")
    List<Photo> findByUserId(@Param("userId") Long userId);

    // Method to find photos by filename (useful for search functionality)
    List<Photo> findByFileNameContainingIgnoreCase(String filename);
}