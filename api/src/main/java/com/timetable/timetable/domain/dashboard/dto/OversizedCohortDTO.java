package com.timetable.timetable.domain.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OversizedCohortDTO {
    private long cohortId;
    private String cohortName;
    private int headcount;
    private int minRequiredCapacity;
    private int compatibleRooms;
    private String severity; // "RED" | "YELLOW"
}
