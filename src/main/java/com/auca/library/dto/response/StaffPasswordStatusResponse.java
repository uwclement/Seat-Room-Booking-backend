package com.auca.library.dto.response;

import java.time.LocalDateTime;
import java.util.List;

import lombok.Data;

@Data
public class StaffPasswordStatusResponse {
    private Long id;
    private String fullName;
    private String email;
    private String employeeId;
    private boolean hasDefaultPassword;
    private LocalDateTime lastPasswordChange;
    private String accountStatus; // ACTIVE, INACTIVE
    private List<String> roles;
    private String passwordStatus; // DEFAULT, USER_SET, EXPIRED
    private boolean passwordEmailSent;
    private LocalDateTime passwordEmailSentAt;
}