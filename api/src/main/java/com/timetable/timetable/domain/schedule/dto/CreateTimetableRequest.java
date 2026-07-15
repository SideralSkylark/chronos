package com.timetable.timetable.domain.schedule.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record CreateTimetableRequest(
    @NotNull(message = "Academic period is required and cannot be empty") int academicYear,
    @NotNull(message = "Semester is required and cannot be empty")
        @Min(value = 1, message = "Semester must be 1 or 2")
        @Max(value = 2, message = "Semester must be 1 or 2")
        int semester) {}
