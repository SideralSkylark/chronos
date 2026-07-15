package com.timetable.timetable.domain.schedule.dto;

import com.timetable.timetable.domain.schedule.entity.Course;
import java.time.LocalDateTime;
import java.util.Map;

public record CourseListResponse(
    Long id,
    String name,
    Long coordinatorId,
    String coordinatorName,
    int years,
    Map<Integer, Integer> expectedCohortsPerYear,
    boolean hasBusinessSimulation,
    Long subjectCount,
    LocalDateTime createdAt,
    LocalDateTime updatedAt) {
  public static CourseListResponse from(Course course, Long subjectCount) {
    return new CourseListResponse(
        course.getId(),
        course.getName(),
        course.getCoordinator().getId(),
        course.getCoordinator().getUsername(),
        course.getYears(),
        course.getExpectedCohortsPerAcademicYear(),
        course.isHasBusinessSimulation(),
        subjectCount,
        course.getCreatedAt(),
        course.getUpdatedAt());
  }
}
