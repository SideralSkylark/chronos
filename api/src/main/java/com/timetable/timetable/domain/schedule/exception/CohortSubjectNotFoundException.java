package com.timetable.timetable.domain.schedule.exception;

import com.timetable.timetable.common.exception.ResourceNotFoundException;

public class CohortSubjectNotFoundException extends ResourceNotFoundException {
  public CohortSubjectNotFoundException(String message) {
    super(message);
  }
}
