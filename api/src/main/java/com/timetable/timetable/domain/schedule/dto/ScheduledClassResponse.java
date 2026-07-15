package com.timetable.timetable.domain.schedule.dto;

import com.timetable.timetable.domain.schedule.entity.ScheduledClass;

public record ScheduledClassResponse(
    Long id,
    Long cohortSubjectId,
    Long timetableId,
    Long roomId,
    Long timeslotId,
    String dayOfWeek,
    String startTime,
    String endTime) {

  public static ScheduledClassResponse from(ScheduledClass sc) {
    return new ScheduledClassResponse(
        sc.getId(),
        sc.getCohortSubject().getId(),
        sc.getTimetable() != null ? sc.getTimetable().getId() : null,
        sc.getRoom().getId(),
        sc.getTimeslot().getId(),
        sc.getTimeslot().getDayOfWeek().name(),
        sc.getTimeslot().getStartTime().toString(),
        sc.getTimeslot().getEndTime().toString());
  }
}
