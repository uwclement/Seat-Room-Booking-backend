package com.auca.library.dto.response;

import lombok.Data;

import java.util.List;

@Data
public class UserResponse {
    private Long id;
    private String fullName;
    private String email;
    private String studentId;
    private List<String> roles;
    private boolean emailVerified;
}