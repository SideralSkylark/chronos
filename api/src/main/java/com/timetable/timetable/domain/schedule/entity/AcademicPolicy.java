package com.timetable.timetable.domain.schedule.entity;

import com.timetable.timetable.domain.user.entity.ApplicationUser;

/**
 * Centralized academic policies.
 *
 * <p>Scheduling is based on institutional contact policy: every discipline meets 2x per week
 * regardless of credits.
 */
public final class AcademicPolicy {

  /** Every discipline meets 2 sessions per week. */
  public static final int SESSIONS_PER_WEEK = 2;

  /**
   * @deprecated Use teacher contract type to determine limit.
   */
  @Deprecated public static final int WEEKLY_TEACHING_HOURS_LIMIT = 12;

  /** Weekly contact hours per discipline for teacher workload calculation. */
  public static final int WEEKLY_CONTACT_HOURS = 4;

  /** Real block duration in minutes. Institutional blocks are 1h45m. */
  public static final int BLOCK_DURATION_MINUTES = 105;

  /** Decimal hours per block for display purposes only. Never use for solver constraints. */
  public static final double BLOCK_DURATION_HOURS = 1.75;

  /**
   * Default generated cohort size used during preprocessing.
   *
   * <p>Chosen to maximize compatibility with the institution's dominant classroom capacity
   * distribution.
   */
  public static final int ESTIMATED_STUDENT_COUNT = 30;

  public static int getWeeklyHoursLimit(ApplicationUser teacher) {
    int limit = teacher.getWeeklyHoursLimit();
    if (limit > 0) {
      return limit;
    }
    // Fallback for edge cases, but should be derived from teacher contract
    return 8;
  }

  /**
   * Estimates weekly contact hours for display purposes only. Uses real block duration (1h45m)
   * rather than the rounded 2h assumption. Do NOT use this for solver constraints or limit
   * comparisons.
   */
  public static double estimateDisplayHours(int weeklySessionCount) {
    return weeklySessionCount * BLOCK_DURATION_HOURS;
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
