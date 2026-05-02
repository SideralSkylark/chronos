package com.timetable.timetable.domain.schedule.dto;

import jakarta.validation.constraints.NotNull;

public record CreateCohortSubjectRequest(
        @NotNull Long cohortId,
        @NotNull Long subjectId,
        @NotNull Long assignedTeacherId) {
    public static CreateCohortSubjectRequest from(Long cohortId, Long subjectId, Long assignedTeacherId) {
        return new CreateCohortSubjectRequest(
                cohortId,
                subjectId,
                assignedTeacherId);
    }
}
