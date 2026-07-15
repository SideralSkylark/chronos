package com.timetable.timetable.auth.exception;

/** Exception thrown when token verification fails. */
public class TokenVerificationException extends RuntimeException {
  public TokenVerificationException(String message) {
    super(message);
  }
}
