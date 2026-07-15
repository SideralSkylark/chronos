package com.timetable.timetable.domain.schedule.dto;

import com.timetable.timetable.domain.schedule.entity.CohortStatus;

public record CohortListResponse(
    Long id,
    int year,
    String section,
    int academicYear,
    int semester,
    Long courseId,
    String courseName,
    int studentCount,
    CohortStatus status) {}
