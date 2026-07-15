package com.timetable.timetable.domain.schedule.exception;

import com.timetable.timetable.common.exception.ResourceNotFoundException;

public class TimetableNotFoundException extends ResourceNotFoundException {
  public TimetableNotFoundException(String message) {
    super(message);
  }
}
