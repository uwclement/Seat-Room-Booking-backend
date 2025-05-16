package com.auca.library.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.auca.library.config.JwtConfig;
import com.auca.library.service.NotificationService;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;

@RestController
@RequestMapping("/api/notifications")
public class NotificationSseController {
    
    @Autowired
    private NotificationService notificationService;
    
    @Autowired
    private JwtConfig jwtConfig;
    
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public ResponseEntity<SseEmitter> streamNotifications(@RequestParam("token") String token) {
        try {
            // Validate JWT token
            String userEmail = validateTokenAndGetEmail(token);
            
            // Create new SSE connection with timeout (30 minutes)
            SseEmitter emitter = new SseEmitter(30 * 60 * 1000L);
            
            // Register this connection with notification service
            notificationService.addNotificationSubscriber(userEmail, emitter);
            
            // Handle connection close/error cleanup
            emitter.onCompletion(() -> notificationService.removeNotificationSubscriber(userEmail, emitter));
            emitter.onTimeout(() -> notificationService.removeNotificationSubscriber(userEmail, emitter));
            emitter.onError((e) -> notificationService.removeNotificationSubscriber(userEmail, emitter));
            
            return ResponseEntity.ok(emitter);
        } catch (JwtException | IllegalArgumentException e) {
            // Return 401 for invalid tokens
            return ResponseEntity.status(401).build();
        }
    }
    
    private String validateTokenAndGetEmail(String token) {
        // Remove "Bearer " prefix if present
        if (token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        
        try {
            // Parse and validate the JWT token
            var claims = Jwts.parserBuilder()
                    .setSigningKey(jwtConfig.getSigningKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            
            // Extract email from token claims
            return claims.getSubject();
        } catch (JwtException e) {
            throw e; // Re-throw to be caught by the calling method
        }
    }
}