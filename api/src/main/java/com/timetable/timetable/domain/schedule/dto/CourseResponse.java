package com.timetable.timetable.domain.schedule.dto;

import com.timetable.timetable.domain.schedule.entity.Course;
import java.time.LocalDateTime;
import java.util.Map;

public record CourseResponse(
    Long id,
    String name,
    Long coordinatorId,
    String coordinatorName,
    int years,
    Map<Integer, Integer> expectedCohortsPerYear,
    boolean hasBusinessSimulation,
    LocalDateTime createdAt,
    LocalDateTime updatedAt) {
  public static CourseResponse from(Course course) {
    return new CourseResponse(
        course.getId(),
        course.getName(),
        course.getCoordinator().getId(),
        course.getCoordinator().getUsername(),
        course.getYears(),
        course.getExpectedCohortsPerAcademicYear(),
        course.isHasBusinessSimulation(),
        course.getCreatedAt(),
        course.getUpdatedAt());
  }
}
