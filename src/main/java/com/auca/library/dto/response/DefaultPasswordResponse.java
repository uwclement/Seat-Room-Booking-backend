package com.auca.library.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DefaultPasswordResponse {
    private String password;
    private String message;
    private boolean isTemporary;
    private String userEmail;
    private String employeeId;
}