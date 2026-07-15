package com.timetable.timetable.domain.schedule.dto;

import com.timetable.timetable.domain.schedule.entity.RoomCourseRestriction;
import com.timetable.timetable.domain.schedule.entity.TimePeriod;

public record RoomRestrictionResponse(
    Long id, Long courseId, String courseName, TimePeriod period) {
  public static RoomRestrictionResponse from(RoomCourseRestriction r) {
    return new RoomRestrictionResponse(
        r.getId(), r.getCourse().getId(), r.getCourse().getName(), r.getPeriod());
  }
}
