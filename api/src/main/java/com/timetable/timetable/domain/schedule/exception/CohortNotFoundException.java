package com.timetable.timetable.domain.schedule.exception;

import com.timetable.timetable.common.exception.ResourceNotFoundException;

public class CohortNotFoundException extends ResourceNotFoundException {
  public CohortNotFoundException(String message) {
    super(message);
  }
}
