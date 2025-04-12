package com.example.demo.repository;

import com.example.demo.model.Planning;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
@Repository
public interface PlanningRepository extends JpaRepository<Planning, Long> {
    List<Planning> findByUserId(Long userId);
    List<Planning> findByUserIdOrderByDayNameAsc(Long userId);
}