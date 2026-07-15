package com.timetable.timetable.domain.schedule.exception;

import com.timetable.timetable.common.exception.ResourceNotFoundException;

public class TimeslotNotFoundException extends ResourceNotFoundException {
  public TimeslotNotFoundException(String message) {
    super(message);
  }
}
