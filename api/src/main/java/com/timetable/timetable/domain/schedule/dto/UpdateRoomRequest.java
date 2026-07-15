package com.timetable.timetable.domain.schedule.dto;

import com.timetable.timetable.domain.schedule.entity.TimePeriod;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.util.Map;
import java.util.Set;

public record UpdateRoomRequest(
    @NotBlank(message = "Name must not be blank.") String name,
    @Min(value = 1, message = "Capacity must be at least one.") int capacity,
    Long restrictedToCourseId,
    Map<TimePeriod, Set<Long>> periodRestrictions) {}
