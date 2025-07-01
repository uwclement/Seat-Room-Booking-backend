package com.auca.library.dto.response;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CourseResponse {
    private Long id;
    private String courseCode;
    private String courseName;
    private Integer creditHours;
    private boolean active;
    private int professorCount;
}