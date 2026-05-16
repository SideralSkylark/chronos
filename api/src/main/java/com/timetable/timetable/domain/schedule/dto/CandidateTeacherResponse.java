package com.timetable.timetable.domain.schedule.dto;

import com.timetable.timetable.domain.user.entity.ApplicationUser;

public record CandidateTeacherResponse(
        Long teacherId,
        String username,
        int currentWeeklyHours,
        int weeklyLimit,
        boolean wouldExceed,
        boolean isEligible,
        int weeklySessionCount,
        double estimatedDisplayHours) {
    public static CandidateTeacherResponse from(
            ApplicationUser teacher, int currentWeeklyHours, int weeklyLimit, boolean wouldExceed, boolean isEligible,
            int weeklySessionCount, double estimatedDisplayHours) {
        return new CandidateTeacherResponse(
            teacher.getId(),
            teacher.getUsername(),
            currentWeeklyHours,
            weeklyLimit,
            wouldExceed,
            isEligible,
            weeklySessionCount,
            estimatedDisplayHours
        );
    }
}
