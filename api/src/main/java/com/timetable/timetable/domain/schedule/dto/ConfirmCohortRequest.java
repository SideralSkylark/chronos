package com.timetable.timetable.domain.schedule.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record ConfirmCohortRequest(
    @NotNull
        @Min(value = 1, message = "student count must be a positive integer")
        @Max(value = 500, message = "student count must not exceed 500")
        int studentCount) {}
