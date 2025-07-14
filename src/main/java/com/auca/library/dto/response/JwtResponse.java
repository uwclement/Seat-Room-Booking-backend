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
    private String identifier; 
    private String userType; 
    private List<String> roles;
    private boolean emailVerified;
    private boolean mustChangePassword;

    public JwtResponse(String accessToken, Long id, String fullName, String email, String identifier, 
                      String userType, boolean emailVerified, boolean mustChangePassword, List<String> roles) {
        this.token = accessToken;
        this.id = id;
        this.fullName = fullName;
        this.email = email;
        this.identifier = identifier;
        this.userType = userType;
        this.emailVerified = emailVerified;
        this.mustChangePassword = mustChangePassword;
        this.roles = roles;
    }
}