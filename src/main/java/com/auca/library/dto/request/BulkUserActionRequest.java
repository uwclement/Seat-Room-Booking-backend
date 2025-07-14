package com.auca.library.dto.request;

import java.util.List;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

@Data
public class BulkUserActionRequest {
    @NotEmpty
    private List<Long> userIds;
    
    private String action; // DELETE, ENABLE, DISABLE, RESET_PASSWORD
}