package com.timetable.timetable.domain.schedule.dto;

import jakarta.validation.constraints.NotNull;

public record CreateCohortSubjectRequest(
        @NotNull(message = "cohort id not specified") Long cohortId,
        @NotNull(message = "subject id not specified") Long subjectId,
        @NotNull(message = "assigned teacher id not specified") Long assignedTeacherId) {
    public static CreateCohortSubjectRequest from(Long cohortId, Long subjectId, Long assignedTeacherId) {
        return new CreateCohortSubjectRequest(
                cohortId,
                subjectId,
                assignedTeacherId);
    }
}
