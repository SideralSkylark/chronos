package com.timetable.timetable.domain.schedule.entity;

import com.timetable.timetable.domain.user.entity.ApplicationUser;

/**
 * Centralized academic policies.
 *
 * Scheduling is based on institutional contact policy:
 * every discipline meets 2x per week regardless of credits.
 */
public final class AcademicPolicy {

    /** Every discipline meets 2 sessions per week. */
    public static final int SESSIONS_PER_WEEK = 2;

    /**
     * Deprecated. Maximum weekly teaching hours per teacher (3 disciplines max).
     */
    public static final int WEEKLY_TEACHING_HOURS_LIMIT = 12;

    /** Weekly contact hours per discipline for teacher workload calculation. */
    public static final int WEEKLY_CONTACT_HOURS = 4;

    /**
     * Default generated cohort size used during preprocessing.
     *
     * Chosen to maximize compatibility with the institution's
     * dominant classroom capacity distribution.
    */
    public static final int ESTIMATED_STUDENT_COUNT = 30;

    public static int getWeeklyHoursLimit(ApplicationUser teacher) {
        int limit = teacher.getWeeklyHoursLimit();
        if (limit > 0) {
            return limit;
        }
        return WEEKLY_TEACHING_HOURS_LIMIT;
    }

    public static int calculateLessonBlocksPerWeek(int credits) {
        return SESSIONS_PER_WEEK;
    }

    public static int calculateWeeklyHours(int credits) {
        return WEEKLY_CONTACT_HOURS;
    }

    public static int calculateWeeklyHours(Subject subject) {
        int blocks = subject.isFixedDaySession() ? 3 : SESSIONS_PER_WEEK;
        int hoursPerBlock = WEEKLY_CONTACT_HOURS / SESSIONS_PER_WEEK;
        return blocks * hoursPerBlock;
    }

    public static int calculateLessonBlocksPerWeek(Subject subject) {
        return subject.isFixedDaySession() ? 3 : SESSIONS_PER_WEEK;
    }

    private AcademicPolicy() {
        throw new AssertionError("Utility class should not be instantiated");
    }
}
