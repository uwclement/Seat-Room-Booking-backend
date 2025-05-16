package com.auca.library.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.auca.library.model.User;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    
    Optional<User> findByStudentId(String studentId);
    
    Optional<User> findByVerificationToken(String token);
    
    Boolean existsByEmail(String email);
    
    Boolean existsByStudentId(String studentId);

    // @Query("SELECT u FROM User u WHERE u.recentNotifications IS NOT NULL AND u.recentNotifications <> ''")
    // List<User> findAllWithNotifications();

    @Query("SELECT u FROM User u WHERE u.emailVerified = true")
    List<User> findActiveUsers();
}