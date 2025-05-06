package com.auca.library.repository;

import com.auca.library.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    
    Optional<User> findByStudentId(String studentId);
    
    Optional<User> findByVerificationToken(String token);
    
    Boolean existsByEmail(String email);
    
    Boolean existsByStudentId(String studentId);
}