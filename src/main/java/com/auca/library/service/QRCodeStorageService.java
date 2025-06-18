package com.auca.library.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class QRCodeStorageService {

    @Value("${qr.storage.type:local}")
    private String storageType;

    @Value("${qr.storage.local.path:./qr-codes}")
    private String localStoragePath;

    @Value("${qr.storage.s3.bucket:}")
    private String s3Bucket;

    @Value("${qr.storage.s3.region:us-east-1}")
    private String s3Region;

    private static final String SEAT_FOLDER = "seats";
    private static final String ROOM_FOLDER = "rooms";

    /**
     * Initialize storage directories
     */
    public void initializeStorage() throws IOException {
        if ("local".equals(storageType)) {
            // Create directories if they don't exist
            Path basePath = Paths.get(localStoragePath);
            Path seatPath = basePath.resolve(SEAT_FOLDER);
            Path roomPath = basePath.resolve(ROOM_FOLDER);

            Files.createDirectories(seatPath);
            Files.createDirectories(roomPath);
        }
        // For S3, initialization would happen in AWS config
    }

    /**
     * Store QR code image
     */
    public String storeQRCode(byte[] imageBytes, String filename, String type) throws IOException {
        if ("local".equals(storageType)) {
            return storeLocal(imageBytes, filename, type);
        } else if ("s3".equals(storageType)) {
            return storeS3(imageBytes, filename, type);
        } else {
            throw new IllegalStateException("Unknown storage type: " + storageType);
        }
    }

    /**
     * Store QR code locally
     */
    private String storeLocal(byte[] imageBytes, String filename, String type) throws IOException {
        // Determine folder based on type
        String folder = "seat".equalsIgnoreCase(type) ? SEAT_FOLDER : ROOM_FOLDER;
        
        // Create full path
        Path basePath = Paths.get(localStoragePath);
        Path folderPath = basePath.resolve(folder);
        Path filePath = folderPath.resolve(filename);

        // Ensure directory exists
        Files.createDirectories(folderPath);

        // Write file
        try (FileOutputStream fos = new FileOutputStream(filePath.toFile())) {
            fos.write(imageBytes);
        }

        // Return relative path
        return folder + "/" + filename;
    }

    /**
     * Store QR code in S3 (placeholder - implement when needed)
     */
    private String storeS3(byte[] imageBytes, String filename, String type) throws IOException {
        // TODO: Implement S3 storage when migrating to cloud
        // This would use AWS SDK to upload to S3
        // For now, throw exception
        throw new UnsupportedOperationException("S3 storage not yet implemented");
    }

    /**
     * Retrieve QR code image
     */
    public byte[] retrieveQRCode(String path) throws IOException {
        if ("local".equals(storageType)) {
            return retrieveLocal(path);
        } else if ("s3".equals(storageType)) {
            return retrieveS3(path);
        } else {
            throw new IllegalStateException("Unknown storage type: " + storageType);
        }
    }

    /**
     * Retrieve QR code from local storage
     */
    private byte[] retrieveLocal(String relativePath) throws IOException {
        Path basePath = Paths.get(localStoragePath);
        Path filePath = basePath.resolve(relativePath);

        if (!Files.exists(filePath)) {
            throw new IOException("QR code file not found: " + relativePath);
        }

        return Files.readAllBytes(filePath);
    }

    /**
     * Retrieve QR code from S3 (placeholder)
     */
    private byte[] retrieveS3(String s3Key) throws IOException {
        // TODO: Implement S3 retrieval when migrating to cloud
        throw new UnsupportedOperationException("S3 retrieval not yet implemented");
    }

    /**
     * Delete QR code
     */
    public boolean deleteQRCode(String path) {
        try {
            if ("local".equals(storageType)) {
                return deleteLocal(path);
            } else if ("s3".equals(storageType)) {
                return deleteS3(path);
            }
        } catch (Exception e) {
            System.err.println("Error deleting QR code: " + e.getMessage());
        }
        return false;
    }

    /**
     * Delete QR code from local storage
     */
    private boolean deleteLocal(String relativePath) throws IOException {
        Path basePath = Paths.get(localStoragePath);
        Path filePath = basePath.resolve(relativePath);

        return Files.deleteIfExists(filePath);
    }

    /**
     * Delete QR code from S3 (placeholder)
     */
    private boolean deleteS3(String s3Key) {
        // TODO: Implement S3 deletion when migrating to cloud
        throw new UnsupportedOperationException("S3 deletion not yet implemented");
    }

    /**
     * Check if QR code exists
     */
    public boolean exists(String path) {
        try {
            if ("local".equals(storageType)) {
                Path basePath = Paths.get(localStoragePath);
                Path filePath = basePath.resolve(path);
                return Files.exists(filePath);
            } else if ("s3".equals(storageType)) {
                // TODO: Implement S3 existence check
                return false;
            }
        } catch (Exception e) {
            System.err.println("Error checking QR code existence: " + e.getMessage());
        }
        return false;
    }

    /**
     * Get full URL for QR code image
     */
    public String getQRCodeUrl(String path) {
        // In production, this would return CDN URL or proper file serving URL
        // For now, return API endpoint that will serve the file
        return "/api/qr/image/" + path;
    }

    /**
     * Archive old QR code when regenerating
     */
    public void archiveQRCode(String currentPath) throws IOException {
        if (!exists(currentPath)) {
            return;
        }

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String archivePath = currentPath.replace(".png", "_archived_" + timestamp + ".png");

        if ("local".equals(storageType)) {
            Path basePath = Paths.get(localStoragePath);
            Path source = basePath.resolve(currentPath);
            Path destination = basePath.resolve(archivePath);
            Files.move(source, destination, StandardCopyOption.REPLACE_EXISTING);
        }
        // Implement for S3 when needed
    }

    /**
     * Clean up old archived QR codes (older than specified days)
     */
    public void cleanupOldQRCodes(int daysToKeep) throws IOException {
        // TODO: Implement cleanup logic for archived QR codes
        // This would scan for archived files older than daysToKeep and delete them
    }
}