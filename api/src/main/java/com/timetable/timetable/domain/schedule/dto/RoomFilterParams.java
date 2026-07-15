package com.timetable.timetable.domain.schedule.dto;

import com.timetable.timetable.domain.schedule.entity.TimePeriod;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class RoomFilterParams {
  private String name;
  private Integer capacityMin;
  private Integer capacityMax;
  private Long courseId;
  private TimePeriod period;
}
