package com.timetable.timetable.domain.schedule.dto;

import java.util.List;

public record PreSolverResult(
    int estimatedCohortsCreated,
    int cohortSubjectsCreated,
    int phantomTeachersCreated,
    List<String> warnings) {
  public boolean hasWarnings() {
    return !warnings.isEmpty();
  }

  boolean hasPhantomTeachers() {
    return phantomTeachersCreated > 0;
  }
}
