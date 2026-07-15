package com.timetable.timetable.domain.schedule.exception;

import com.timetable.timetable.common.exception.BusinessValidationException;

public class InvalidStudentRoleException extends BusinessValidationException {
  public InvalidStudentRoleException(String message) {
    super(message);
  }
}
