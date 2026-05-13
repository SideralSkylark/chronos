package com.timetable.timetable.domain.schedule.dto;

import java.util.List;

public record AcademicPeriodDto(Integer year, List<Integer> semesters) {
}
