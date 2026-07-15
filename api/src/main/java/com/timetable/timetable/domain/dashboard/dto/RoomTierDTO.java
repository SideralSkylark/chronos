package com.timetable.timetable.domain.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RoomTierDTO {
  private String label;
  private long roomCount;
  private double supplyPercent;
  private double demandPercent;
  private String severity; // "RED" | "YELLOW" | "GREEN"
}
