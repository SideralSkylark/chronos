package com.timetable.timetable.domain.schedule.dto;

/** used to derive the number of cohorts for the given years(ie: 1st, 2nd, 3rd, ...) of a course */
public record CohortEstimationConfig(
    int cohortsYear1, int cohortsYear2, int cohortsYear3Plus, int estimatedStudentsPerCohort) {
  /**
   * standard config. defaults to 3 cohorts in the first year, and 2 for the remaining years,
   * assumes 35 students per cohort
   */
  public static CohortEstimationConfig defaultConfig() {
    return new CohortEstimationConfig(2, 2, 2, 35);
  }

  /** personalised config. year 2 and up default to 2 cohorts */
  public static CohortEstimationConfig forCourse(int cohortsYear1) {
    return new CohortEstimationConfig(cohortsYear1, 2, 2, 35);
  }

  /** personalised config, cohort size default is 35 */
  public static CohortEstimationConfig forCourse(
      int cohortsYear1, int cohortsYear2, int cohortsYear3Plus) {
    return new CohortEstimationConfig(cohortsYear1, cohortsYear2, cohortsYear3Plus, 35);
  }

  public int cohortsForYear(int year) {
    return switch (year) {
      case 1 -> cohortsYear1;
      case 2 -> cohortsYear2;
      default -> cohortsYear3Plus;
    };
  }
}
