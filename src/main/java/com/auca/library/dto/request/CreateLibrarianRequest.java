package com.auca.library.dto.request;

import com.auca.library.model.Location;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.DayOfWeek;
import java.util.Set;

@Data
public class CreateLibrarianRequest {
    @NotBlank(message = "Full name is required")
    @Size(max = 100)
    private String fullName;
    
    @NotBlank(message = "Email is required")
    @Size(max = 50)
    @Email
    private String email;
    
    @NotBlank(message = "Employee ID is required")
    @Size(max = 20)
    private String employeeId;
    
    @Size(max = 15)
    private String phone;
    
    @NotNull(message = "Location is required")
    private Location location;
    
    @NotEmpty(message = "At least one working day must be specified")
    private Set<DayOfWeek> workingDays;
    
    private boolean activeThisWeek = false;
    
    private boolean isDefaultLibrarian = false;
    
}
