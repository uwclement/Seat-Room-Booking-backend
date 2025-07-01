package com.auca.library.dto.response;

import lombok.Getter;
import lombok.Setter;
import java.util.List;

@Getter
@Setter
public class LabClassResponse {
    private Long id;
    private String labNumber;
    private String name;
    private String description;
    private Integer capacity;
    private String building;
    private String floor;
    private boolean available;
    private List<Long> equipmentIds;
    private List<String> equipmentNames;
    private int equipmentCount;
}