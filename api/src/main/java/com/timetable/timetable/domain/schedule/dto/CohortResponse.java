package com.timetable.timetable.domain.schedule.dto;

import com.timetable.timetable.domain.schedule.entity.Cohort;
import com.timetable.timetable.domain.schedule.entity.CohortStatus;
import java.util.List;
import java.util.stream.Collectors;

public record CohortResponse(
    Long id,
    int year,
    String section,
    int academicYear,
    int semester,
    Long courseId,
    String courseName,
    CohortStatus status,
    int studentCount,
    List<Long> studentIds) {
  public static CohortResponse from(Cohort cohort) {
    return new CohortResponse(
        cohort.getId(),
        cohort.getYear(),
        cohort.getSection(),
        cohort.getAcademicYear(),
        cohort.getSemester(),
        cohort.getCourse().getId(),
        cohort.getCourse().getName(),
        cohort.getStatus(),
        cohort.getEstimatedStudentCount(),
        cohort.getStudents().stream().map(student -> student.getId()).collect(Collectors.toList()));
  }
}
