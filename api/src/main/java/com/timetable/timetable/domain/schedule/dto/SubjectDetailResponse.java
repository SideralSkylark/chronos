package com.timetable.timetable.domain.schedule.dto;

import com.timetable.timetable.domain.schedule.entity.Subject;
import java.util.List;

public record SubjectDetailResponse(
    Long id,
    String name,
    int credits,
    int targetYear,
    int targetSemester,
    Long courseId,
    String courseName,
    List<TeacherInfo> eligibleTeachers) {
  public static SubjectDetailResponse from(Subject subject) {
    return new SubjectDetailResponse(
        subject.getId(),
        subject.getName(),
        subject.getCredits(),
        subject.getTargetYear(),
        subject.getTargetSemester(),
        subject.getCourse().getId(),
        subject.getCourse().getName(),
        subject.getEligibleTeachers().stream()
            .map(t -> new TeacherInfo(t.getId(), t.getUsername()))
            .toList());
  }

  public record TeacherInfo(Long id, String username) {}
}
