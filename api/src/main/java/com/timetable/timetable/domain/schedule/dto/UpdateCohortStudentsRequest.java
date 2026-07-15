package com.timetable.timetable.domain.schedule.dto;

import jakarta.validation.constraints.NotNull;
import java.util.List;

public record UpdateCohortStudentsRequest(
    @NotNull(message = "students to be updated not specified") List<Long> studentIds) {}
