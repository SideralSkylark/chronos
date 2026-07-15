package com.timetable.timetable.domain.schedule.dto;

import com.timetable.timetable.domain.schedule.entity.CohortStatus;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class CohortFilterParams {
  private String name;
  private Long courseId;
  private Integer academicYear;
  private Integer semester;
  private CohortStatus status;
}
