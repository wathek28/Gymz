package com.example.demo.repository;

import com.example.demo.model.Role;
import com.example.demo.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    // Rechercher un utilisateur par numéro de téléphone

    boolean existsByPhoneNumber(String phoneNumber);
    Optional<User> findById(Long id);

    List<User> findByRole(Role role);
    boolean existsByEmailAndIdNot(String email, Long userId);
    Optional<User> findByEmail(String email);
    Optional<User> findByPhoneNumber(String phoneNumber);
    @Query("SELECT u FROM User u LEFT JOIN FETCH u.photos WHERE u.phoneNumber = :phoneNumber")
    Optional<User> findByPhoneNumberWithPhotos(@Param("phoneNumber") String phoneNumber);

    Optional<Object> findByIdAndRole(Long id, Role role);

}
