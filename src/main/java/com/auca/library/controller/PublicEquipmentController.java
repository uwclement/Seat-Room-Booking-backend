package com.auca.library.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.auca.library.dto.response.CourseResponse;
import com.auca.library.dto.response.EquipmentResponse;
import com.auca.library.dto.response.LabClassResponse;
import com.auca.library.service.CourseService;
import com.auca.library.service.EquipmentService;
import com.auca.library.service.LabClassService;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/public")
public class PublicEquipmentController {

    @Autowired
    private EquipmentService equipmentService;
    
    @Autowired
    private LabClassService labClassService;
    
    @Autowired
    private CourseService courseService;

    // Students can see equipment allowed for them
    @GetMapping("/equipment/student-allowed")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<List<EquipmentResponse>> getStudentAllowedEquipment() {
        List<EquipmentResponse> equipment = equipmentService.getStudentAllowedEquipment();
        return ResponseEntity.ok(equipment);
    }

    // Professors can see all available equipment
    @GetMapping("/equipment/available")
    @PreAuthorize("hasRole('PROFESSOR')")
    public ResponseEntity<List<EquipmentResponse>> getAvailableEquipment() {
        List<EquipmentResponse> equipment = equipmentService.getAvailableEquipment();
        return ResponseEntity.ok(equipment);
    }

    // Professors can see available lab classes
    @GetMapping("/lab-classes/available")
    @PreAuthorize("hasRole('PROFESSOR')")
    public ResponseEntity<List<LabClassResponse>> getAvailableLabClasses() {
        List<LabClassResponse> labClasses = labClassService.getAvailableLabClasses();
        return ResponseEntity.ok(labClasses);
    }

    // Get active courses for professor selection
    @GetMapping("/courses/active")
    @PreAuthorize("hasRole('PROFESSOR')")
    public ResponseEntity<List<CourseResponse>> getActiveCourses() {
        List<CourseResponse> courses = courseService.getActiveCourses();
        return ResponseEntity.ok(courses);
    }
}