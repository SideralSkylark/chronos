package com.timetable.timetable.domain.schedule.dto;

import com.timetable.timetable.domain.schedule.entity.Timetable;
import com.timetable.timetable.domain.schedule.entity.TimetableStatus;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

public record TimetableResponse(
    Long id,
    List<Long> scheduledClassesIds,
    Integer scheduledClassesCount,
    TimetableStatus status,
    int academicYear,
    int semester,
    LocalDateTime createdAt,
    LocalDateTime updatedAt) {
  public static TimetableResponse from(Timetable timetable) {
    List<Long> scheduledClassesIds =
        timetable.getScheduledClasses() != null
            ? timetable.getScheduledClasses().stream()
                .map(slot -> slot.getId())
                .collect(Collectors.toList())
            : List.of();

    return new TimetableResponse(
        timetable.getId(),
        scheduledClassesIds,
        scheduledClassesIds.size(),
        timetable.getStatus(),
        timetable.getAcademicYear(),
        timetable.getSemester(),
        timetable.getCreatedAt(),
        timetable.getUpdatedAt());
  }
}
