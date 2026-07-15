package com.timetable.timetable.domain.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TeacherWorkloadDTO {
  private long teacherId;
  private String teacherName;
  private long totalHours;
  private long weeklyHoursLimit;
  private boolean overloaded;
  private int weeklySessionCount;
  private double estimatedDisplayHours;
}
