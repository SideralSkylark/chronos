package com.timetable.timetable.domain.schedule.dto;

import java.util.List;

/** request used to load or create relevant data before the solver's processing */
public record PreSolverRequest(
    int academicYear,
    int semester,
    List<Long> courseIds, // null = todos
    CohortEstimationConfig estimationConfig, // null = default
    PhantomTeacherPolicy policy // default: CREATE_IF_NEEDED
    ) {
  /**
   * default pre solver request. courseIds are defaulted to null, uses {@link
   * CohortEstimationConfig} default config and {@link PhantomTeacherPolicy} create if
   * CREATE_IF_NEEDED
   */
  public static PreSolverRequest withDefaults(int academicYear, int semester) {
    return new PreSolverRequest(
        academicYear,
        semester,
        null,
        CohortEstimationConfig.defaultConfig(),
        PhantomTeacherPolicy.CREATE_IF_NEEDED);
  }

  /**
   * customizable presolver request check {@link CohortEstimationConfig} and {@link
   * PhantomTeacherPolicy}
   */
  public static PreSolverRequest custom(
      int academicYear,
      int semester,
      CohortEstimationConfig estimationConfig,
      PhantomTeacherPolicy policy) {
    return new PreSolverRequest(academicYear, semester, null, estimationConfig, policy);
  }
}
