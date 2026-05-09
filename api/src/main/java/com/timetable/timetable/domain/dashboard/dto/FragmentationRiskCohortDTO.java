package com.timetable.timetable.domain.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FragmentationRiskCohortDTO {
    private long cohortId;
    private String cohortName;
    private int headcount;
    private int maxCompatibleCapacity;
    private double utilizationPercent;
}
