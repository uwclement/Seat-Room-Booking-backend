package com.auca.library.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.auca.library.dto.response.MessageResponse;
import com.auca.library.service.NotificationService;

import lombok.Data;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/admin/notifications")
@PreAuthorize("hasRole('ADMIN')")
public class AdminNotificationController {

    @Autowired
    private NotificationService notificationService;
    
    @PostMapping("/library-info")
    public ResponseEntity<MessageResponse> sendLibraryInfo(@RequestBody LibraryInfoRequest request) {
        notificationService.sendLibraryInfoNotification(request.getTitle(), request.getMessage());
        return ResponseEntity.ok(new MessageResponse("Library information notification sent to all active users"));
    }
}

@Data
class LibraryInfoRequest {
    private String title;
    private String message;
}