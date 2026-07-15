package com.timetable.timetable.domain.schedule.dto;

import jakarta.validation.constraints.NotNull;

public record CreateScheduledClassRequest(
    @NotNull(message = "cohort subject must be specified") Long cohortSubjectId,
    Long timetableId, // optional
    @NotNull(message = "room ID must be specified") Long roomId,
    @NotNull(message = "timeslot ID must be specified") Long timeslotId) {}
