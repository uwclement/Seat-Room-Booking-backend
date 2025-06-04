package com.auca.library.dto.request;

import lombok.Data;
import jakarta.validation.constraints.NotNull;

@Data
public class InvitationResponseRequest {
    @NotNull(message = "Response is required")
    private Boolean accepted;
    private String message;
}