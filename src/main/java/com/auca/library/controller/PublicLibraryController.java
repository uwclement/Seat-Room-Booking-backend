package com.auca.library.controller;

import com.auca.library.dto.response.LibraryStatusResponse;
import com.auca.library.dto.response.LibraryScheduleResponse;
import com.auca.library.service.LibraryScheduleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/library")
public class PublicLibraryController {

    @Autowired
    private LibraryScheduleService libraryScheduleService;

    // Get current library status (open/closed)
    @GetMapping("/status")
    public ResponseEntity<LibraryStatusResponse> getCurrentStatus() {
        return ResponseEntity.ok(libraryScheduleService.getCurrentLibraryStatus());
    }

    // Get current week's schedule
    @GetMapping("/schedule")
    public ResponseEntity<List<LibraryScheduleResponse>> getCurrentSchedule() {
        return ResponseEntity.ok(libraryScheduleService.getAllLibrarySchedules());
    }
}