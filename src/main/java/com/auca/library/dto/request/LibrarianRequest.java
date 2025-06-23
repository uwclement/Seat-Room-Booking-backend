package com.auca.library.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDate;

@Data
public class LibrarianRequest {

    @NotBlank(message = "Name is required")
    private String name;

    @NotBlank(message = "Phone number is required")
    private String phone;

    private boolean activeToday;

    private boolean isDefault;

    private LocalDate workingDay;
}
