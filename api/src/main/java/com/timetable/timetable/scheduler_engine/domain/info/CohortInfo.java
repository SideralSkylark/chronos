package com.timetable.timetable.scheduler_engine.domain.info;

import lombok.*;

/**
 * Represents a student group (cohort) in the scheduling problem. This is a lightweight, immutable
 * representation for the solver.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CohortInfo {

  /** Unique identifier from the Cohort entity */
  private Long id;

  /** Human-readable name (e.g., "1ano-Engenharia Informatica-A-2026") */
  private String displayName;

  /** Number of students in this cohort (for room capacity validation) */
  private int studentCount;

  /** The course/program this cohort belongs to (for room restrictions) */
  private Long courseId;

  /** The cohort's current year (1, 2, 3, etc.) */
  private int year;

  /** The section identifier (A, B, C, etc.) */
  private String section;

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof CohortInfo)) return false;
    CohortInfo that = (CohortInfo) o;
    return id != null && id.equals(that.id);
  }

  @Override
  public int hashCode() {
    return id != null ? id.hashCode() : 0;
  }

  @Override
  public String toString() {
    return "CohortInfo{"
        + "id="
        + id
        + ", name='"
        + displayName
        + '\''
        + ", students="
        + studentCount
        + '}';
  }
}
