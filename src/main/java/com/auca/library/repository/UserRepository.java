package com.auca.library.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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


    @Query("SELECT u FROM User u JOIN u.roles r WHERE r.name = 'ROLE_PROFESSOR' AND u.professorApproved = false")
List<User> findPendingProfessors();

@Query("SELECT u FROM User u JOIN u.roles r WHERE r.name = 'ROLE_PROFESSOR' AND u.professorApproved = true")
List<User> findApprovedProfessors();

@Query("SELECT u FROM User u JOIN u.roles r WHERE r.name = 'ROLE_EQUIPMENT_ADMIN'")
Optional<User> findEquipmentAdmin();

@Query("SELECT u FROM User u JOIN u.roles r WHERE r.name = 'ROLE_HOD'")
Optional<User> findHod();

@Query("SELECT u FROM User u JOIN u.roles r WHERE r.name = 'ROLE_PROFESSOR' " +
       "AND EXISTS (SELECT er FROM EquipmentRequest er WHERE er.user = u AND er.createdAt >= :since)")
List<User> findActiveProfessors(@Param("since") LocalDateTime since);
}