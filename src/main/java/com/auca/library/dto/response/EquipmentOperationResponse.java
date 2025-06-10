package com.auca.library.dto.response;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EquipmentOperationResponse {
    private Integer successCount;
    private Integer failureCount;
    private List<String> errors;
    private List<String> warnings;
    
    public EquipmentOperationResponse() {
        this.successCount = 0;
        this.failureCount = 0;
    }
    
    public EquipmentOperationResponse(int successCount, int failureCount, List<String> errors) {
        this.successCount = successCount;
        this.failureCount = failureCount;
        this.errors = errors;
    }
}