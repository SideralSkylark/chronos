package com.timetable.timetable.auth.exception;

import com.timetable.timetable.common.exception.ResourceAlreadyExistsException;

/**
 * Exception thrown when an attempt is made to register a user with an email or username that
 * already exists.
 */
public class UserAlreadyExistsException extends ResourceAlreadyExistsException {
  public UserAlreadyExistsException(String message) {
    super(message);
  }
}
