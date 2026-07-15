package com.timetable.timetable.domain.schedule.exception;

import com.timetable.timetable.common.exception.ResourceNotFoundException;

public class SubjectNotFoundException extends ResourceNotFoundException {
  public SubjectNotFoundException(String message) {
    super(message);
  }
}
