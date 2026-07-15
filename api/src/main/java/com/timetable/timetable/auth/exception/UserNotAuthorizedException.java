package com.timetable.timetable.auth.exception;

/** Exception thrown when a user attempts to perform an action they are not authorized for. */
public class UserNotAuthorizedException extends RuntimeException {

  public UserNotAuthorizedException(String message) {
    super(message);
  }
}
