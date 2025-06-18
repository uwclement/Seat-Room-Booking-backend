package com.auca.library.controller;

import com.auca.library.service.QRCodeStorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/qr/image")
@CrossOrigin(origins = "*", maxAge = 3600)
@Tag(name = "QR Code Images", description = "Public endpoints for serving QR code images")
public class QRCodeImageController {

    @Autowired
    private QRCodeStorageService qrStorageService;

    /**
     * Serve QR code image - Public endpoint
     */
    @GetMapping("/{type}/{filename}")
    @Operation(summary = "Get QR code image", description = "Retrieve QR code image by path")
    public ResponseEntity<Resource> getQRCodeImage(
            @PathVariable String type,
            @PathVariable String filename) {
        
        try {
            String path = type + "/" + filename;
            byte[] imageData = qrStorageService.retrieveQRCode(path);
            
            ByteArrayResource resource = new ByteArrayResource(imageData);
            
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                    .contentType(MediaType.IMAGE_PNG)
                    .contentLength(imageData.length)
                    .body(resource);
                    
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }
}