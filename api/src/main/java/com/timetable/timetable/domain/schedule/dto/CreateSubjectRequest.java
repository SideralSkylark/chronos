package com.timetable.timetable.domain.schedule.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record CreateSubjectRequest(
    @NotBlank(message = "Subject name is required and cannot be empty")
        @Size(min = 2, max = 150, message = "Subject name must be between 2 and 150 characters")
        String name,
    @NotNull(message = "credits are required") int credits,
    @NotNull(message = "target year is required") int targetYear,
    @NotNull(message = "target semester is required")
        @Min(value = 1, message = "Semester must be 1 or 2")
        @Max(value = 2, message = "Semester must be 1 or 2")
        int targetSemester,
    @NotNull(message = "Course ID is required") Long courseId,
    List<Long> eligibleTeacherIds) {}
