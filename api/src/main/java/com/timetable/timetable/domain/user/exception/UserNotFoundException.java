package com.timetable.timetable.domain.user.exception;

import com.timetable.timetable.common.exception.ResourceNotFoundException;

public class UserNotFoundException extends ResourceNotFoundException {
  public UserNotFoundException(String message) {
    super(message);
  }
}
