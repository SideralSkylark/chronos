package com.timetable.timetable.domain.schedule.dto;

import com.timetable.timetable.domain.schedule.entity.Cohort;
import java.util.List;

/** response object for estimated cohort */
public record CohortEstimationResult(
    List<Cohort> cohorts, List<String> warnings, boolean wasGenerated) {}
