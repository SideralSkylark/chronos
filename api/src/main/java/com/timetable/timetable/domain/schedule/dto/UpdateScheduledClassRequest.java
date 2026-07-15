package com.timetable.timetable.domain.schedule.dto;

import jakarta.validation.constraints.NotNull;

public record UpdateScheduledClassRequest(
    @NotNull(message = "cohort subject id is required") Long cohortSubjectId,
    Long timetableId,
    @NotNull(message = "room ID is required") Long roomId,
    @NotNull(message = "timeslot ID is required") Long timeslotId) {}
