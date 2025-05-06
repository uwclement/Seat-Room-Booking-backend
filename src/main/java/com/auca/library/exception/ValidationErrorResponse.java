package com.auca.library.exception;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Date;
import java.util.Map;

@Data
@AllArgsConstructor
public class ValidationErrorResponse {
    private Date timestamp;
    private String message;
    private Map<String, String> errors;
}