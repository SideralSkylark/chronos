package com.timetable.timetable.domain.user.exception;

public class UserNotAuthorizedException extends RuntimeException {
  public UserNotAuthorizedException(String message) {
    super(message);
  }
}
