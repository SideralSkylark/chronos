package com.timetable.timetable.domain.schedule.exception;

import com.timetable.timetable.common.exception.ResourceNotFoundException;

public class ScheduledClassNotFoundException extends ResourceNotFoundException {
  public ScheduledClassNotFoundException(String message) {
    super(message);
  }
}
