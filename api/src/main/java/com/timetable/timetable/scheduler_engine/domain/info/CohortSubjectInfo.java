package com.timetable.timetable.scheduler_engine.domain.info;

import lombok.*;

/**
 * Represents a specific subject being taught to a specific cohort by a specific teacher. This is
 * the "lesson instance" that needs to be scheduled multiple times per week.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CohortSubjectInfo {

  /** Unique identifier from the CohortSubject entity */
  private Long id;

  /** The student group taking this subject */
  private CohortInfo cohort;

  /** The subject being taught */
  private SubjectInfo subject;

  /** The teacher assigned to teach this subject to this cohort */
  private TeacherInfo teacher;

  /**
   * How many lesson blocks per week are needed for this cohort-subject. Pre-calculated from
   * AcademicPolicy based on subject credits.
   */
  private int lessonBlocksPerWeek;

  /** Returns a human-readable description */
  public String getDisplayName() {
    if (cohort == null || subject == null || teacher == null) {
      return "Incomplete CohortSubject";
    }
    return cohort.getDisplayName() + " - " + subject.getName() + " (" + teacher.getName() + ")";
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof CohortSubjectInfo)) return false;
    CohortSubjectInfo that = (CohortSubjectInfo) o;
    return id != null && id.equals(that.id);
  }

  @Override
  public int hashCode() {
    return id != null ? id.hashCode() : 0;
  }

  @Override
  public String toString() {
    return "CohortSubjectInfo{"
        + "id="
        + id
        + ", cohort="
        + (cohort != null ? cohort.getDisplayName() : "null")
        + ", subject="
        + (subject != null ? subject.getName() : "null")
        + ", teacher="
        + (teacher != null ? teacher.getName() : "null")
        + ", blocksPerWeek="
        + lessonBlocksPerWeek
        + '}';
  }
}
