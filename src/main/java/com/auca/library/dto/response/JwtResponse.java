package com.auca.library.dto.response;

import lombok.Data;

import java.util.List;

@Data
public class JwtResponse {
    private String token;
    private String type = "Bearer";
    private Long id;
    private String fullName;
    private String email;
    private String studentId;
    private List<String> roles;
    private boolean emailVerified;

    public JwtResponse(String accessToken, Long id, String fullName, String email, String studentId, 
                      boolean emailVerified, List<String> roles) {
        this.token = accessToken;
        this.id = id;
        this.fullName = fullName;
        this.email = email;
        this.studentId = studentId;
        this.emailVerified = emailVerified;
        this.roles = roles;
    }
}