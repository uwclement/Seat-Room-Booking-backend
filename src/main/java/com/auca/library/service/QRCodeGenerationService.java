package com.auca.library.service;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.imageio.ImageIO;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageConfig;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

@Service
public class QRCodeGenerationService {

    // @Value("${qr.base-url:http://localhost:8080}")
    @Value("${qr.base-url:http://192.168.1.71:8080}")
    private String baseUrl;

    @Value("${qr.generation.size:300}")
    private int qrSize;

    @Value("${qr.generation.margin:10}")
    private int qrMargin;

    @Value("${qr.generation.logo.enabled:true}")
    private boolean logoEnabled;

    @Value("${qr.generation.logo.path:classpath:static/logo.png}")
    private String logoPath;

    private static final String DATE_FORMAT = "yyyyMMdd_HHmmss";
    private static final int LOGO_SIZE_RATIO = 5; // Logo will be 1/5 of QR code size

    /**
     * Generate a unique token for QR code
     */
    public String generateUniqueToken() {
        return UUID.randomUUID().toString();
    }

    /**
     * Generate QR code URL for seats
     */
    public String generateSeatQRUrl(String token) {
        return String.format("%s/scan?type=seat&token=%s", baseUrl, token);
    }

    /**
     * Generate QR code URL for rooms
     */
    public String generateRoomQRUrl(String token) {
        return String.format("%s/scan?type=room&token=%s", baseUrl, token);
    }

    /**
     * Generate QR code image and return as byte array
     */
    public byte[] generateQRCodeImage(String content, String overlayText) throws WriterException, IOException {
        // Configure QR code generation
        Map<EncodeHintType, Object> hints = new HashMap<>();
        hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H); // High error correction for logo overlay
        hints.put(EncodeHintType.MARGIN, qrMargin);
        hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");

        // Generate QR code
        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        BitMatrix bitMatrix = qrCodeWriter.encode(content, BarcodeFormat.QR_CODE, qrSize, qrSize, hints);

        // Convert to BufferedImage
        MatrixToImageConfig config = new MatrixToImageConfig(0xFF000000, 0xFFFFFFFF);
        BufferedImage qrImage = MatrixToImageWriter.toBufferedImage(bitMatrix, config);

        // Add logo if enabled
        if (logoEnabled) {
            qrImage = addLogoToQRCode(qrImage);
        }

        // Add overlay text (seat/room number)
        if (overlayText != null && !overlayText.isEmpty()) {
            qrImage = addTextToQRCode(qrImage, overlayText);
        }

        // Convert to byte array
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(qrImage, "PNG", baos);
        return baos.toByteArray();
    }

    /**
     * Generate QR code and save to file
     */
    public String generateAndSaveQRCode(String content, String type, String identifier) throws WriterException, IOException {
        // Generate filename
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern(DATE_FORMAT));
        String filename = String.format("QR_%s_%s_%s.png", type.toUpperCase(), identifier, timestamp);

        // Generate QR code image
        byte[] qrImageBytes = generateQRCodeImage(content, identifier);

        // Return filename for storage service to handle
        return filename;
    }

    /**
     * Add logo to QR code center
     */
    private BufferedImage addLogoToQRCode(BufferedImage qrImage) throws IOException {
        try {
            // Load logo - handle both classpath and file system paths
            BufferedImage logo = null;
            if (logoPath.startsWith("classpath:")) {
                String resourcePath = logoPath.substring("classpath:".length());
                var logoStream = getClass().getClassLoader().getResourceAsStream(resourcePath);
                if (logoStream != null) {
                    logo = ImageIO.read(logoStream);
                }
            } else {
                File logoFile = new File(logoPath);
                if (logoFile.exists()) {
                    logo = ImageIO.read(logoFile);
                }
            }

            if (logo == null) {
                return qrImage; // Return original if logo not found
            }

            // Calculate logo size (1/5 of QR code)
            int logoSize = qrSize / LOGO_SIZE_RATIO;

            // Scale logo
            Image scaledLogo = logo.getScaledInstance(logoSize, logoSize, Image.SCALE_SMOOTH);

            // Create graphics context
            Graphics2D graphics = qrImage.createGraphics();
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // Calculate position (center)
            int x = (qrSize - logoSize) / 2;
            int y = (qrSize - logoSize) / 2;

            // Draw white background for logo (improves visibility)
            graphics.setColor(Color.WHITE);
            graphics.fillRect(x - 5, y - 5, logoSize + 10, logoSize + 10);

            // Draw logo
            graphics.drawImage(scaledLogo, x, y, null);
            graphics.dispose();

            return qrImage;
        } catch (Exception e) {
            // Log error and return original image
            System.err.println("Error adding logo to QR code: " + e.getMessage());
            return qrImage;
        }
    }

    /**
     * Add text overlay to QR code
     */
    private BufferedImage addTextToQRCode(BufferedImage qrImage, String text) {
        // Create a new image with extra space for text
        int newHeight = qrSize + 40; // Add 40 pixels for text
        BufferedImage combined = new BufferedImage(qrSize, newHeight, BufferedImage.TYPE_INT_RGB);

        // Create graphics context
        Graphics2D g = combined.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        // Fill background
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, qrSize, newHeight);

        // Draw QR code
        g.drawImage(qrImage, 0, 0, null);

        // Draw text
        g.setColor(Color.BLACK);
        g.setFont(new Font("Arial", Font.BOLD, 16));
        
        // Center text
        FontMetrics fm = g.getFontMetrics();
        int textWidth = fm.stringWidth(text);
        int x = (qrSize - textWidth) / 2;
        int y = qrSize + 25;
        
        g.drawString(text, x, y);
        g.dispose();

        return combined;
    }

    /**
     * Generate batch QR codes
     */
    public Map<String, byte[]> generateBatchQRCodes(Map<String, String> contentMap) throws WriterException, IOException {
        Map<String, byte[]> results = new HashMap<>();
        
        for (Map.Entry<String, String> entry : contentMap.entrySet()) {
            String identifier = entry.getKey();
            String content = entry.getValue();
            byte[] qrImage = generateQRCodeImage(content, identifier);
            results.put(identifier, qrImage);
        }
        
        return results;
    }

    /**
     * Validate QR code content
     */
    public boolean isValidQRContent(String content) {
        if (content == null || content.trim().isEmpty()) {
            return false;
        }
        
        // Check if it matches our expected URL pattern
        String pattern = String.format("^%s/scan\\?type=(seat|room)&token=[a-f0-9\\-]{36}$", 
           baseUrl.replace(".", "\\.").replace("/", "\\/"));
        return content.matches(pattern);
    }
}