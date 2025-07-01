package com.auca.library.dto.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;
import java.util.List;

@Getter
@Setter
public class ProfessorCourseRequest {
    @NotEmpty(message = "At least one course must be selected")
    private List<Long> courseIds;
}