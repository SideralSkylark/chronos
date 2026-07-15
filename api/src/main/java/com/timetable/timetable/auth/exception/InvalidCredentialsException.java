package com.timetable.timetable.auth.exception;

/** Exception thrown when the provided credentials (email or password) are incorrect. */
public class InvalidCredentialsException extends RuntimeException {
  public InvalidCredentialsException(String message) {
    super(message);
  }
}
