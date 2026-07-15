package com.timetable.timetable.domain.schedule.exception;

import com.timetable.timetable.common.exception.ResourceAlreadyExistsException;

public class CohortAlreadyExistsException extends ResourceAlreadyExistsException {
  public CohortAlreadyExistsException(String message) {
    super(message);
  }
}
