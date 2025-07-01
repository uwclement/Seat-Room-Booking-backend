package com.auca.library.controller;

import com.auca.library.dto.request.ProfessorCourseRequest;
import com.auca.library.dto.response.MessageResponse;
import com.auca.library.dto.response.ProfessorResponse;
import com.auca.library.model.Course;
import com.auca.library.service.ProfessorService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/professor")
public class ProfessorController {

    @Autowired
    private ProfessorService professorService;

    // Request course approval
    @PostMapping("/request-courses")
    @PreAuthorize("hasRole('PROFESSOR')")
    public ResponseEntity<MessageResponse> requestCourseApproval(
            @Valid @RequestBody ProfessorCourseRequest request,
            Authentication authentication) {
        MessageResponse response = professorService.requestCourseApproval(request, authentication.getName());
        return ResponseEntity.ok(response);
    }

    // Get approved courses
    @GetMapping("/my-courses")
    @PreAuthorize("hasRole('PROFESSOR')")
    public ResponseEntity<List<Course>> getMyApprovedCourses(Authentication authentication) {
        List<Course> courses = professorService.getProfessorApprovedCourses(authentication.getName());
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

    @PostMapping("/{professorId}/approve-courses")
    @PreAuthorize("hasRole('HOD')")
    public ResponseEntity<MessageResponse> approveProfessorCourses(
            @PathVariable Long professorId,
            @RequestBody List<Long> courseIds,
            Authentication authentication) {
        MessageResponse response = professorService.approveProfessorCourses(professorId, courseIds, authentication.getName());
        return ResponseEntity.ok(response);
    }
}