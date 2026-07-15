package com.timetable.timetable.scheduler_engine.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class LessonDTO {
  private String time; // ex: "08:00-09:00"
  private String room;
  private String cohort;
  private String teacher;
}
