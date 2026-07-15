package com.timetable.timetable.domain.schedule.dto;

import jakarta.validation.constraints.NotNull;

public record ReasignTeacherRequest(
    @NotNull(message = "teacher id not specified") Long teacherId) {}
