package com.timetable.timetable.domain.schedule.entity;

import java.time.LocalTime;

public enum TimePeriod {
  MORNING, // 07:00-12:00
  AFTERNOON, // 12:00-18:00
  EVENING; // 18:00-22:00

  public static TimePeriod fromStartTime(LocalTime startTime) {
    if (startTime.isBefore(LocalTime.of(12, 0))) {
      return MORNING;
    } else if (startTime.isBefore(LocalTime.of(18, 0))) {
      return AFTERNOON;
    } else {
      return EVENING;
    }
  }
}
