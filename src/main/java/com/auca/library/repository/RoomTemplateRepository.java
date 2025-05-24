package com.auca.library.repository;

import com.auca.library.model.RoomCategory;
import com.auca.library.model.RoomTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RoomTemplateRepository extends JpaRepository<RoomTemplate, Long> {
    
    Optional<RoomTemplate> findByTemplateName(String templateName);
    
    boolean existsByTemplateName(String templateName);
    
    List<RoomTemplate> findByCategory(RoomCategory category);
    
    @Query("SELECT t FROM RoomTemplate t WHERE " +
           "LOWER(t.templateName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(t.description) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<RoomTemplate> searchTemplates(@Param("keyword") String keyword);
    
    @Query("SELECT t FROM RoomTemplate t ORDER BY t.createdAt DESC")
    List<RoomTemplate> findAllOrderByCreatedAtDesc();
}