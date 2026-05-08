package com.timetable.timetable.domain.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DashboardStatsDTO {
    private long totalRoomCapacity;
    private long totalRoomCount;
    
    private long totalCohortDemand;
    private long totalCohortCount;
    private long largestCohort;
    private long smallestCohort;
    private double averageCohortSize;
    private int bottleneckYear;
    private List<YearBreakdownDTO> cohortsByYear;
    
    private long totalTeachers;
    private long teachersOverloaded;
    private long totalAssignedSlots;
    private double avgSlotsPerTeacher;
    
    private String solverReadiness;
    private String solverReadinessReason;
    private long capacityMargin;

    private long morningDemand;
    private long afternoonDemand;
    private String morningReadiness;
    private String afternoonReadiness;
    private String morningReadinessReason;
    private String afternoonReadinessReason;

    private List<CourseRankDTO> topCoursesByCohorts;
    private List<TeacherWorkloadDTO> mostLoadedTeachers;
    private List<TeacherWorkloadDTO> leastLoadedTeachers;
}