package com.auca.library.dto.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class UserRoleUpdateRequest {
    @NotEmpty
    private List<String> roles;
}