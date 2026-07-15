package com.timetable.timetable.domain.schedule.exception;

import com.timetable.timetable.common.exception.ResourceNotFoundException;

public class RoomNotFoundException extends ResourceNotFoundException {
  public RoomNotFoundException(String message) {
    super(message);
  }
}
