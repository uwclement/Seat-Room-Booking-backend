package com.auca.library.controller;

import java.time.LocalDate;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.auca.library.dto.response.LibrarianResponse;
import com.auca.library.service.LibrarianService;


@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/librarians")
public class LibrarianController {

    @Autowired
    private LibrarianService librarianService;

    @GetMapping("/default-or-active")
    public ResponseEntity<LibrarianResponse> getDefaultOrActive(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(librarianService.getActiveOrDefaultLibrarian(date));
    }
    
}