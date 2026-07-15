package com.timetable.timetable.domain.schedule.dto;

import jakarta.validation.constraints.NotNull;

public record UpdateCohortSubjectRequest(
    @NotNull Long assignedTeacherId, @NotNull Boolean isActive) {
  public static UpdateCohortSubjectRequest from(Long assignedTeacherId, boolean isActive) {
    return new UpdateCohortSubjectRequest(assignedTeacherId, isActive);
  }
}
