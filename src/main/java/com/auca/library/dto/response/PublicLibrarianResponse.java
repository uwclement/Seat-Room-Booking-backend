package com.auca.library.dto.response;

import com.auca.library.model.Location;
import lombok.Data;

@Data
public class PublicLibrarianResponse {
    private String fullName;
    private String email;
    private String phone;
    private Location location;
    private String locationDisplayName;
    private boolean isDefaultLibrarian;
}
