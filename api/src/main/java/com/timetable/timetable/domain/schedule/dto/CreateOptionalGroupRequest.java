package com.timetable.timetable.domain.schedule.dto;

import jakarta.validation.constraints.NotNull;

public record CreateOptionalGroupRequest(
    @NotNull(message = "Id for subject 1 not specified") Long s1,
    @NotNull(message = "Id for subject 2 not specified") Long s2) {}
