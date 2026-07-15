package com.timetable.timetable.auth.exception;

/** Exception thrown when a token is malformed, missing, or otherwise invalid. */
public class InvalidTokenException extends RuntimeException {
  public InvalidTokenException(String message) {
    super(message);
  }
}
