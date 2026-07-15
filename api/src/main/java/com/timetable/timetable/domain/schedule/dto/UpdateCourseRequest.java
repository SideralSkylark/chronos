package com.timetable.timetable.domain.schedule.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.Map;

public record UpdateCourseRequest(
    @NotBlank(message = "Course name must not be empty.") String name,
    @NotNull(message = "Corse coordinator no specified.") Long coordinatorId,
    @Positive(message = "Course span must be positive") Integer years,
    Map<Integer, Integer> expectedCohortsPerYear,
    boolean hasBusinessSimulation) {}
