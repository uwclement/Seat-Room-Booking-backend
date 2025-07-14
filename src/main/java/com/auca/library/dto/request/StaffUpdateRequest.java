package com.auca.library.dto.request;

import java.time.LocalDate;

import com.auca.library.model.Location;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class StaffUpdateRequest {
    @NotBlank
    @Size(min = 3, max = 100)
    private String fullName;

    @NotBlank
    @Size(max = 50)
    @Email
    private String email;

    @NotBlank
    @Size(min = 3, max = 20)
    private String employeeId;

    @NotBlank
    @Size(max = 15)
    private String phone;

    @NotNull
    private Location location;

    // Librarian-specific fields
    private LocalDate workingDay;
    private boolean activeToday = false;
    private boolean isDefault = false;
}