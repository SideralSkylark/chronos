package com.timetable.timetable.domain.schedule.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.List;

public record UpdateCohortRequest(
    @NotNull(message = "students year must be specified")
        @Positive(message = "students year must be positive")
        int year,
    @NotBlank(message = "section must not be empty") String section,
    @NotNull(message = "academic year must be specified")
        @Positive(message = "academic year must be positive")
        int academicYear,
    @NotNull(message = "semester must be specified")
        @Min(value = 1, message = "Semester must be 1 or 2")
        @Max(value = 2, message = "Semester must be 1 or 2")
        int semester,
    @NotEmpty(message = "At least one student must be assigned to the cohort")
        List<Long> studentIds) {}
