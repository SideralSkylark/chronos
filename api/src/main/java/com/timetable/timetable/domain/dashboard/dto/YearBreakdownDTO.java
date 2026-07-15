package com.timetable.timetable.domain.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class YearBreakdownDTO {
  private int year;
  private long cohortCount;
  private long totalStudents;
}
