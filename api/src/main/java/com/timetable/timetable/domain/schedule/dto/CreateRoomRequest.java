package com.timetable.timetable.domain.schedule.dto;

import com.timetable.timetable.domain.schedule.entity.TimePeriod;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.Map;
import java.util.Set;

public record CreateRoomRequest(
    @NotBlank(message = "Name must not be empty.") String name,
    @NotNull(message = "Capacity must not be empty.")
        @Min(value = 1, message = "Capacity must be at leat 1.")
        int capacity,
    Long restrictedToCourseId,
    Map<TimePeriod, Set<Long>> periodRestrictions) {}
