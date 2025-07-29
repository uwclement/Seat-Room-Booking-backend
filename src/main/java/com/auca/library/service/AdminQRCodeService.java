package com.auca.library.service;

import com.auca.library.controller.AdminQRCodeController.QRBulkDownloadRequest;
import com.auca.library.dto.request.QRBulkGenerationRequest;
import com.auca.library.dto.response.*;
import com.auca.library.exception.ResourceNotFoundException;
import com.auca.library.model.*;
import com.auca.library.repository.*;
import com.google.zxing.WriterException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
public class AdminQRCodeService {

    @Autowired
    private SeatRepository seatRepository;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private QRCodeLogRepository qrCodeLogRepository;

    @Autowired
    private QRCodeGenerationService qrGenerationService;

    @Autowired
    private QRCodeStorageService qrStorageService;

    /**
     * Generate QR code for a seat
     */
    @Transactional
    public QRCodeGenerationResponse generateSeatQRCode(Long seatId, String adminEmail) throws WriterException, IOException {
        Seat seat = seatRepository.findById(seatId)
                .orElseThrow(() -> new ResourceNotFoundException("Seat not found"));
        
        User admin = userRepository.findByEmail(adminEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Admin user not found"));

        // Check if QR code already exists
        String oldToken = seat.getQrCodeToken();
        
        // Generate new token
        String newToken = qrGenerationService.generateUniqueToken();
        String qrUrl = qrGenerationService.generateSeatQRUrl(newToken);
        
        // Generate QR code image
        byte[] qrImage = qrGenerationService.generateQRCodeImage(qrUrl, seat.getSeatNumber());
        
        // Save QR code image
        String filename = qrGenerationService.generateAndSaveQRCode(qrUrl, "SEAT", seat.getSeatNumber().replaceAll("[\\n\\r\\t\\s]+", "_"));
        String imagePath = qrStorageService.storeQRCode(qrImage, filename, "seat");
        
        // Archive old QR code if exists
        if (seat.getQrImagePath() != null && qrStorageService.exists(seat.getQrImagePath())) {
            qrStorageService.archiveQRCode(seat.getQrImagePath());
        }
        
        // Update seat
        seat.setQrCodeToken(newToken);
        seat.setQrCodeUrl(qrUrl);
        seat.setQrImagePath(imagePath);
        seat.setQrGeneratedAt(LocalDateTime.now());
        Integer currentVersion = seat.getQrVersion();
        seat.setQrVersion(currentVersion == null ? 1 : currentVersion + 1);
        seatRepository.save(seat);
        
        qrCodeLogRepository.markAllAsNotCurrentForResource("SEAT", seatId);
        // Log generation
        QRCodeLog log = new QRCodeLog("SEAT", seatId, admin, newToken);
        log.setOldToken(oldToken);
        log.setQrVersion(seat.getQrVersion());
        log.setGenerationReason(oldToken == null ? "Initial generation" : "Regeneration");
        qrCodeLogRepository.save(log);
        
        // Mark old logs as not current
        if (oldToken != null) {
            QRCodeLog oldLog = qrCodeLogRepository.findCurrentQRCode("SEAT", seatId);
            if (oldLog != null) {
                oldLog.setCurrent(false);
                qrCodeLogRepository.save(oldLog);
            }
        }
        
        // Create response
        QRCodeGenerationResponse response = new QRCodeGenerationResponse();
        response.setSuccess(true);
        response.setResourceType("SEAT");
        response.setResourceId(seatId);
        response.setResourceIdentifier(seat.getSeatNumber());
        response.setQrCodeUrl(qrUrl);
        response.setQrCodeToken(newToken);
        response.setImagePath(qrStorageService.getQRCodeUrl(imagePath));
        response.setGeneratedAt(seat.getQrGeneratedAt());
        response.setVersion(seat.getQrVersion());
        
        return response;
    }

    /**
     * Generate QR code for a room
     */
    @Transactional
    public QRCodeGenerationResponse generateRoomQRCode(Long roomId, String adminEmail) throws WriterException, IOException {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("Room not found"));
        
        User admin = userRepository.findByEmail(adminEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Admin user not found"));

        // Check if QR code already exists
        String oldToken = room.getQrCodeToken();
        
        // Generate new token
        String newToken = qrGenerationService.generateUniqueToken();
        String qrUrl = qrGenerationService.generateRoomQRUrl(newToken);
        
        // Generate QR code image
        byte[] qrImage = qrGenerationService.generateQRCodeImage(qrUrl, room.getRoomNumber());
        
        // Save QR code image
        String filename = qrGenerationService.generateAndSaveQRCode(qrUrl, "ROOM", room.getRoomNumber());
        String imagePath = qrStorageService.storeQRCode(qrImage, filename, "room");
        
        // Archive old QR code if exists
        if (room.getQrImagePath() != null && qrStorageService.exists(room.getQrImagePath())) {
            qrStorageService.archiveQRCode(room.getQrImagePath());
        }
        
        // Update room
        room.setQrCodeToken(newToken);
        room.setQrCodeUrl(qrUrl);
        room.setQrImagePath(imagePath);
        room.setQrGeneratedAt(LocalDateTime.now());
        Integer currentVersion = room.getQrVersion();
        room.setQrVersion(currentVersion == null ? 1 : currentVersion + 1);
        roomRepository.save(room);
        
        // Log generation
        QRCodeLog log = new QRCodeLog("ROOM", roomId, admin, newToken);
        log.setOldToken(oldToken);
        log.setQrVersion(room.getQrVersion());
        log.setGenerationReason(oldToken == null ? "Initial generation" : "Regeneration");
        qrCodeLogRepository.save(log);
        
        // Mark old logs as not current
        if (oldToken != null) {
            QRCodeLog oldLog = qrCodeLogRepository.findCurrentQRCode("ROOM", roomId);
            if (oldLog != null) {
                oldLog.setCurrent(false);
                qrCodeLogRepository.save(oldLog);
            }
        }
        
        // Create response
        QRCodeGenerationResponse response = new QRCodeGenerationResponse();
        response.setSuccess(true);
        response.setResourceType("ROOM");
        response.setResourceId(roomId);
        response.setResourceIdentifier(room.getRoomNumber());
        response.setQrCodeUrl(qrUrl);
        response.setQrCodeToken(newToken);
        response.setImagePath(qrStorageService.getQRCodeUrl(imagePath));
        response.setGeneratedAt(room.getQrGeneratedAt());
        response.setVersion(room.getQrVersion());
        
        return response;
    }
    



    // get seat for bulk selection 
    private List<Seat> getSeatsForBulkGeneration(QRBulkGenerationRequest request) {
    if (request.getResourceIds() != null && !request.getResourceIds().isEmpty()) {
        // Specific seats requested
        return seatRepository.findAllById(request.getResourceIds());
    } else if (request.isGenerateForAll()) {
        // All enabled seats
        return seatRepository.findByIsDisabled(false);
    } else if (request.isGenerateForMissing()) {
        // Only seats without QR codes
        return seatRepository.findSeatsWithoutQRCode();
    } else {
        return new ArrayList<>();
    }
}

    
// bulk set QRcode generation
@Transactional
public BulkQRGenerationResponse bulkGenerateSeatQRCodes(QRBulkGenerationRequest request, String adminEmail) {
    User admin = userRepository.findByEmail(adminEmail)
            .orElseThrow(() -> new ResourceNotFoundException("Admin user not found"));
    
    BulkQRGenerationResponse response = new BulkQRGenerationResponse();
    response.setStartTime(LocalDateTime.now());
    
    List<Long> successfulIds = new ArrayList<>();
    List<String> errors = new ArrayList<>();
    Map<String, byte[]> generatedQRCodes = new HashMap<>();
    
    List<Seat> seats = getSeatsForBulkGeneration(request);
    
    for (Seat seat : seats) {
        try {
            boolean shouldGenerate = request.isRegenerateExisting() || 
                                   seat.getQrImagePath() == null || 
                                   seat.getQrImagePath().trim().isEmpty();
            
            if (!shouldGenerate) {
                continue; // Skip this seat
            }
            
            // Generate QR code
            String newToken = qrGenerationService.generateUniqueToken();
            String qrUrl = qrGenerationService.generateSeatQRUrl(newToken);
            byte[] qrImage = qrGenerationService.generateQRCodeImage(qrUrl, seat.getSeatNumber());
            String filename = qrGenerationService.generateAndSaveQRCode(qrUrl, "SEAT", seat.getSeatNumber());
            String imagePath = qrStorageService.storeQRCode(qrImage, filename, "seat");
            
            // Update seat
            seat.setQrCodeToken(newToken);
            seat.setQrCodeUrl(qrUrl);
            seat.setQrImagePath(imagePath);
            seat.setQrGeneratedAt(LocalDateTime.now());
            seat.setQrVersion(seat.getQrVersion() != null ? seat.getQrVersion() + 1 : 1);
            seatRepository.save(seat);
            
            // Log generation
            QRCodeLog log = new QRCodeLog("SEAT", seat.getId(), admin, newToken);
            log.setGenerationReason("Bulk generation");
            log.setQrVersion(seat.getQrVersion());
            qrCodeLogRepository.save(log);
            
            successfulIds.add(seat.getId());
            
            // Store for download if requested
            if (request.isGenerateAndDownload()) {
                generatedQRCodes.put(seat.getSeatNumber() + "_QR.png", qrImage);
            }
            
        } catch (Exception e) {
            errors.add("Failed to generate QR for seat " + seat.getSeatNumber() + ": " + e.getMessage());
        }
    }
    
    response.setEndTime(LocalDateTime.now());
    response.setTotalRequested(seats.size());
    response.setSuccessCount(successfulIds.size());
    response.setFailureCount(errors.size());
    response.setSuccessfulResourceIds(successfulIds);
    response.setErrors(errors);
    response.setGeneratedQRCodes(generatedQRCodes);
    response.setDownloadAvailable(request.isGenerateAndDownload() && !generatedQRCodes.isEmpty());
    response.setDownloadMessage(response.getSuccessCount() + " QR codes generated successfully");
    
    return response;
}


 // get Room for bulk selection 
  private List<Room> getRoomsForBulkGeneration(QRBulkGenerationRequest request) {
    if (request.getResourceIds() != null && !request.getResourceIds().isEmpty()) {
        // Specific rooms requested
        return roomRepository.findAllById(request.getResourceIds());
    } else if (request.isGenerateForAll()) {
        // All available rooms
        return roomRepository.findByAvailableTrue();
    } else if (request.isGenerateForMissing()) {
        // Only rooms without QR codes
        return roomRepository.findRoomsWithoutQRCode();
    } else {
        return new ArrayList<>();
    }
}

    // bulkGenerateRoomQRCodes 
@Transactional
public BulkQRGenerationResponse bulkGenerateRoomQRCodes(QRBulkGenerationRequest request, String adminEmail) {
    User admin = userRepository.findByEmail(adminEmail)
            .orElseThrow(() -> new ResourceNotFoundException("Admin user not found"));
    
    BulkQRGenerationResponse response = new BulkQRGenerationResponse();
    response.setStartTime(LocalDateTime.now());
    
    List<Long> successfulIds = new ArrayList<>();
    List<String> errors = new ArrayList<>();
    Map<String, byte[]> generatedQRCodes = new HashMap<>();
    
    List<Room> rooms = getRoomsForBulkGeneration(request);
    
    for (Room room : rooms) {
        try {
            boolean shouldGenerate = request.isRegenerateExisting() || 
                                   room.getQrImagePath() == null || 
                                   room.getQrImagePath().trim().isEmpty();
            
            if (!shouldGenerate) {
                continue; // Skip this room
            }
            
            // Generate QR code
            String newToken = qrGenerationService.generateUniqueToken();
            String qrUrl = qrGenerationService.generateRoomQRUrl(newToken);
            byte[] qrImage = qrGenerationService.generateQRCodeImage(qrUrl, room.getRoomNumber());
            String filename = qrGenerationService.generateAndSaveQRCode(qrUrl, "ROOM", room.getRoomNumber());
            String imagePath = qrStorageService.storeQRCode(qrImage, filename, "room");
            
            // Update room
            room.setQrCodeToken(newToken);
            room.setQrCodeUrl(qrUrl);
            room.setQrImagePath(imagePath);
            room.setQrGeneratedAt(LocalDateTime.now());
            room.setQrVersion(room.getQrVersion() != null ? room.getQrVersion() + 1 : 1);
            roomRepository.save(room);
            
            // Log generation
            QRCodeLog log = new QRCodeLog("ROOM", room.getId(), admin, newToken);
            log.setGenerationReason("Bulk generation");
            log.setQrVersion(room.getQrVersion());
            qrCodeLogRepository.save(log);
            
            successfulIds.add(room.getId());
            
            // Store for download if requested
            if (request.isGenerateAndDownload()) {
                generatedQRCodes.put(room.getRoomNumber() + "_QR.png", qrImage);
            }
            
        } catch (Exception e) {
            errors.add("Failed to generate QR for room " + room.getRoomNumber() + ": " + e.getMessage());
            e.printStackTrace(); // For debugging
        }
    }
    
    response.setEndTime(LocalDateTime.now());
    response.setTotalRequested(rooms.size());
    response.setSuccessCount(successfulIds.size());
    response.setFailureCount(errors.size());
    response.setSuccessfulResourceIds(successfulIds);
    response.setErrors(errors);
    response.setGeneratedQRCodes(generatedQRCodes);
    response.setDownloadAvailable(request.isGenerateAndDownload() && !generatedQRCodes.isEmpty());
    response.setDownloadMessage(response.getSuccessCount() + " QR codes generated successfully");
    
    return response;
}

// Helper method to check if QR should be generated for room
private boolean shouldGenerateQRForRoom(Room room, QRBulkGenerationRequest request) {
    // Always generate if regenerateExisting is true
    if (request.isRegenerateExisting()) {
        return true;
    }
    
    if (room.getQrImagePath() == null || room.getQrImagePath().trim().isEmpty()) {
        return true;
    }
    if (!qrStorageService.exists(room.getQrImagePath())) {
        return true;
    }
    
    return false;
}


    

    /**
     * Download QR code image
     */
    public ResponseEntity<Resource> downloadQRCode(String type, Long resourceId) throws IOException {
        byte[] qrImage;
        String filename;
        
        if ("seat".equalsIgnoreCase(type)) {
            Seat seat = seatRepository.findById(resourceId)
                    .orElseThrow(() -> new ResourceNotFoundException("Seat not found"));
            
            if (seat.getQrImagePath() == null) {
                throw new ResourceNotFoundException("QR code not generated for this seat");
            }
            
            qrImage = qrStorageService.retrieveQRCode(seat.getQrImagePath());
            filename = "QR_SEAT_" + seat.getSeatNumber() + ".png";
            
        } else if ("room".equalsIgnoreCase(type)) {
            Room room = roomRepository.findById(resourceId)
                    .orElseThrow(() -> new ResourceNotFoundException("Room not found"));
            
            if (room.getQrImagePath() == null) {
                throw new ResourceNotFoundException("QR code not generated for this room");
            }
            
            qrImage = qrStorageService.retrieveQRCode(room.getQrImagePath());
            filename = "QR_ROOM_" + room.getRoomNumber() + ".png";
            
        } else {
            throw new IllegalArgumentException("Invalid resource type");
        }
        
        ByteArrayResource resource = new ByteArrayResource(qrImage);
        
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.IMAGE_PNG)
                .contentLength(qrImage.length)
                .body(resource);
    }


    /**
 * Download selected QR codes as ZIP
 */
public ResponseEntity<Resource> downloadSelectedQRCodes(QRBulkDownloadRequest request) throws IOException {
    Map<String, byte[]> qrCodes = new HashMap<>();
    
    if ("SEAT".equalsIgnoreCase(request.getType()) || "SEATS".equalsIgnoreCase(request.getType())) {
        List<Seat> seats = getSeatsForDownload(request);
        
        for (Seat seat : seats) {
            if (seat.getQrImagePath() != null && !seat.getQrImagePath().isEmpty()) {
                try {
                    byte[] qrImage = qrStorageService.retrieveQRCode(seat.getQrImagePath());
                    qrCodes.put(seat.getSeatNumber() + "_QR.png", qrImage);
                } catch (Exception e) {
                    System.err.println("Failed to read QR code for seat " + seat.getSeatNumber() + ": " + e.getMessage());
                    // Continue with other QR codes
                }
            }
        }
        
    } else if ("ROOM".equalsIgnoreCase(request.getType()) || "ROOMS".equalsIgnoreCase(request.getType())) {
        List<Room> rooms = getRoomsForDownload(request);
        
        for (Room room : rooms) {
            if (room.getQrImagePath() != null && !room.getQrImagePath().isEmpty()) {
                try {
                    byte[] qrImage = qrStorageService.retrieveQRCode(room.getQrImagePath());
                    qrCodes.put(room.getRoomNumber() + "_QR.png", qrImage);
                } catch (Exception e) {
                    System.err.println("Failed to read QR code for room " + room.getRoomNumber() + ": " + e.getMessage());
                    // Continue with other QR codes
                }
            }
        }
    } else {
        throw new IllegalArgumentException("Invalid resource type: " + request.getType());
    }
    
    if (qrCodes.isEmpty()) {
        throw new FileNotFoundException("No QR codes found for download");
    }
    
    return createZipDownload(qrCodes, request.getType());
}

/**
 * Get seats for download based on request
 */
private List<Seat> getSeatsForDownload(QRBulkDownloadRequest request) {
    if (request.isDownloadAll()) {
        return seatRepository.findByIsDisabled(false);
    } else if (request.getResourceIds() != null && !request.getResourceIds().isEmpty()) {
        return seatRepository.findAllById(request.getResourceIds());
    } else {
        return new ArrayList<>();
    }
}

/**
 * Get rooms for download based on request
 */
private List<Room> getRoomsForDownload(QRBulkDownloadRequest request) {
    if (request.isDownloadAll()) {
        return roomRepository.findByAvailableTrue();
    } else if (request.getResourceIds() != null && !request.getResourceIds().isEmpty()) {
        return roomRepository.findAllById(request.getResourceIds());
    } else {
        return new ArrayList<>();
    }
}

/**
 * Create ZIP file from QR codes map
 */
private ResponseEntity<Resource> createZipDownload(Map<String, byte[]> qrCodes, String type) throws IOException {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    
    try (ZipOutputStream zos = new ZipOutputStream(baos)) {
        for (Map.Entry<String, byte[]> entry : qrCodes.entrySet()) {
            ZipEntry zipEntry = new ZipEntry(entry.getKey());
            zos.putNextEntry(zipEntry);
            zos.write(entry.getValue());
            zos.closeEntry();
        }
    }
    
    byte[] zipBytes = baos.toByteArray();
    ByteArrayResource resource = new ByteArrayResource(zipBytes);
    
    String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
    String fileName = type.toLowerCase() + "_qrcodes_" + timestamp + ".zip";
    
    return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
            .contentType(MediaType.APPLICATION_OCTET_STREAM)
            .contentLength(zipBytes.length)
            .body(resource);
}

    
     // Download bulk QR codes as ZIP
     
     public ResponseEntity<Resource> downloadBulkQRCodes(Map<String, byte[]> qrCodes, String type) throws IOException {
    if (qrCodes.isEmpty()) {
        throw new IOException("No QR codes available for download");
    }
    
    return createZipDownload(qrCodes, type);
}

    /**
     * Get QR code statistics
     */
    public QRCodeStatisticsResponse getQRCodeStatistics() {
        QRCodeStatisticsResponse stats = new QRCodeStatisticsResponse();
        
        // Seat statistics
        long totalSeats = seatRepository.count();
        long seatsWithQR = seatRepository.findAll().stream()
                .filter(s -> s.getQrCodeToken() != null)
                .count();
        stats.setTotalSeats(totalSeats);
        stats.setSeatsWithQRCode(seatsWithQR);
        stats.setSeatsWithoutQRCode(totalSeats - seatsWithQR);
        
        // Room statistics
        long totalRooms = roomRepository.count();
        long roomsWithQR = roomRepository.findAll().stream()
                .filter(r -> r.getQrCodeToken() != null)
                .count();
        stats.setTotalRooms(totalRooms);
        stats.setRoomsWithQRCode(roomsWithQR);
        stats.setRoomsWithoutQRCode(totalRooms - roomsWithQR);
        
        // Generation statistics
        LocalDateTime lastWeek = LocalDateTime.now().minusWeeks(1);
        stats.setQrCodesGeneratedLastWeek(qrCodeLogRepository.countGeneratedSince(lastWeek));
        
        // Recent generations
        List<QRCodeLog> recentLogs = qrCodeLogRepository.findAll().stream()
                .sorted((a, b) -> b.getGeneratedAt().compareTo(a.getGeneratedAt()))
                .limit(10)
                .toList();
        
        stats.setRecentGenerations(recentLogs.stream()
                .map(this::mapToLogResponse)
                .toList());
        
        return stats;
    }

    /**
     * Get QR generation history
     */
    public List<QRCodeLogResponse> getQRGenerationHistory(String type, Long resourceId) {
        List<QRCodeLog> logs;
        
        if (type != null && resourceId != null) {
            logs = qrCodeLogRepository.findByResourceTypeAndResourceId(type.toUpperCase(), resourceId);
        } else if (type != null) {
            logs = qrCodeLogRepository.findRecentByType(type.toUpperCase());
        } else {
            logs = qrCodeLogRepository.findAll();
        }
        
        return logs.stream()
                .sorted((a, b) -> b.getGeneratedAt().compareTo(a.getGeneratedAt()))
                .map(this::mapToLogResponse)
                .toList();
    }

    private QRCodeLogResponse mapToLogResponse(QRCodeLog log) {
        QRCodeLogResponse response = new QRCodeLogResponse();
        response.setId(log.getId());
        response.setResourceType(log.getResourceType());
        response.setResourceId(log.getResourceId());
        response.setQrVersion(log.getQrVersion());
        response.setGeneratedBy(log.getGeneratedBy().getFullName());
        response.setGeneratedAt(log.getGeneratedAt());
        response.setCurrent(log.isCurrent());
        response.setGenerationReason(log.getGenerationReason());
        return response;
    }
}