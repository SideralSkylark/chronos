package com.timetable.timetable.domain.schedule.dto;

import com.timetable.timetable.domain.schedule.entity.TimetableStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record UpdateTimetableRequest(
    @NotNull(message = "academic year is required") int academicYear,
    @NotNull(message = "semester is required")
        @Min(value = 1, message = "Semester must be 1 or 2")
        @Max(value = 2, message = "Semester must be 1 or 2")
        int semester,
    @NotNull(message = "Status is required") TimetableStatus status) {}
