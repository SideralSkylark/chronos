package com.timetable.timetable.domain.schedule.exception;

import com.timetable.timetable.common.exception.ResourceNotFoundException;

public class CourseNotFoundException extends ResourceNotFoundException {
  public CourseNotFoundException(String message) {
    super(message);
  }
}
