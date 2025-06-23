package com.auca.library.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.auca.library.dto.request.LibrarianRequest;
import com.auca.library.dto.response.LibrarianResponse;
import com.auca.library.dto.response.MessageResponse;
import com.auca.library.service.LibrarianService;

import jakarta.validation.Valid;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/admin/librarians")
@PreAuthorize("hasRole('ADMIN')")
public class AdminLibrarianController {

    @Autowired
    private LibrarianService librarianService;

    @GetMapping
    public ResponseEntity<List<LibrarianResponse>> getAllLibrarians() {
        return ResponseEntity.ok(librarianService.getAllLibrarians());
    }

    @GetMapping("/active")
    public ResponseEntity<List<LibrarianResponse>> getActiveLibrariansToday(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(librarianService.getActiveLibrariansForDay(date));
    }

    @GetMapping("/{id}")
    public ResponseEntity<LibrarianResponse> getLibrarianById(@PathVariable Long id) {
        return ResponseEntity.ok(librarianService.getLibrarianById(id));
    }

    @PostMapping
    public ResponseEntity<LibrarianResponse> createLibrarian(@Valid @RequestBody LibrarianRequest request) {
        return ResponseEntity.ok(librarianService.createLibrarian(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<LibrarianResponse> updateLibrarian(
            @PathVariable Long id, @Valid @RequestBody LibrarianRequest request) {
        return ResponseEntity.ok(librarianService.updateLibrarian(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<MessageResponse> deleteLibrarian(@PathVariable Long id) {
        return ResponseEntity.ok(librarianService.deleteLibrarian(id));
    }

    @GetMapping("/default-or-active")
    public ResponseEntity<LibrarianResponse> getDefaultOrActive(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(librarianService.getActiveOrDefaultLibrarian(date));
    }
}
