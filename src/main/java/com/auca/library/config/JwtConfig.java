package com.auca.library.config;

import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

@Configuration
public class JwtConfig {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expirationMs}")
    private int jwtExpirationMs;

    private SecretKey key;

    // Generate a secure key for HS512 algorithm
    public SecretKey getSigningKey() {
        if (key == null) {
            // If a secret is provided in properties, use it (ensuring it's properly sized)
            if (jwtSecret != null && !jwtSecret.isEmpty()) {
                // Convert string to bytes
                byte[] keyBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
                
                // Check if the key is long enough
                if (keyBytes.length >= 64) { // 512 bits = 64 bytes
                    key = Keys.hmacShaKeyFor(keyBytes);
                } else {
                    // If key is too short, generate a secure one instead
                    key = Keys.secretKeyFor(SignatureAlgorithm.HS512);
                    System.out.println("WARNING: Provided JWT secret is too short for HS512. Using a generated key instead.");
                }
            } else {
                // If no secret provided, generate a secure one
                key = Keys.secretKeyFor(SignatureAlgorithm.HS512);
            }
        }
        return key;
    }

    public int getJwtExpirationMs() {
        return jwtExpirationMs;
    }
}