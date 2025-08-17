package com.auca.library.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.auca.library.dto.request.ProfessorCourseRequest;
import com.auca.library.dto.response.CourseResponse;
import com.auca.library.dto.response.MessageResponse;
import com.auca.library.dto.response.ProfessorResponse;
import com.auca.library.service.CourseService;
import com.auca.library.service.ProfessorService;

import jakarta.validation.Valid;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/professor")
public class ProfessorController {

    @Autowired
    private ProfessorService professorService;
    
    @Autowired
    private CourseService courseService;

    @GetMapping("courses/active")
    public ResponseEntity<List<CourseResponse>> getAllCourses() {
        List<CourseResponse> courses = courseService.getActiveCourses();
        return ResponseEntity.ok(courses);
    }

    // Get approved courses
    @GetMapping("/my-courses")
    @PreAuthorize("hasRole('PROFESSOR')")
    public ResponseEntity<List<CourseResponse>> getMyApprovedCourses(Authentication authentication) {
    List<CourseResponse> courses = professorService.getProfessorApprovedCourses(authentication.getName());
    return ResponseEntity.ok(courses);
    }

    // HOD endpoints
    @GetMapping("/pending-approvals")
    @PreAuthorize("hasRole('HOD')")
    public ResponseEntity<List<ProfessorResponse>> getPendingProfessorApprovals() {
        List<ProfessorResponse> professors = professorService.getPendingProfessorApprovals();
        return ResponseEntity.ok(professors);
    }

    @PostMapping("/{professorId}/approve-account")
    @PreAuthorize("hasRole('HOD')")
    public ResponseEntity<MessageResponse> approveProfessorAccount(
            @PathVariable Long professorId,
            Authentication authentication) {
        MessageResponse response = professorService.approveProfessorAccount(professorId, authentication.getName());
        return ResponseEntity.ok(response);
    }

    

    @PostMapping("/{professorId}/reject-account")
    @PreAuthorize("hasRole('HOD')")
        public ResponseEntity<MessageResponse> rejectProfessorAccount(
            @PathVariable Long professorId,
            @RequestBody String rejectionReason,
            Authentication authentication) {
            MessageResponse response = professorService.rejectProfessorAccount(
            professorId, rejectionReason, authentication.getName());
         return ResponseEntity.ok(response);
    }


}