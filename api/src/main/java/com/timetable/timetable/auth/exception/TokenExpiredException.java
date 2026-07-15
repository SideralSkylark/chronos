package com.timetable.timetable.auth.exception;

/** Exception thrown when a token has expired. */
public class TokenExpiredException extends RuntimeException {
  public TokenExpiredException(String message) {
    super(message);
  }
}
