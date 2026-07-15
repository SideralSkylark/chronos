package com.timetable.timetable.domain.schedule.exception;

public class RoomCapacityExceededException extends RuntimeException {
  public RoomCapacityExceededException(String message) {
    super(message);
  }
}
