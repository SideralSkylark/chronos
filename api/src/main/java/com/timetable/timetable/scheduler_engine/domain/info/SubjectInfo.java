package com.timetable.timetable.scheduler_engine.domain.info;

import jakarta.annotation.Nullable;
import lombok.*;

/**
 * Represents an academic subject/course in the scheduling problem. This is a
 * lightweight, immutable
 * representation for the solver.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubjectInfo {

  /** Unique identifier from the Subject entity */
  private Long id;

  /** Subject name (e.g., "Algoritmos e Estruturas de Dados") */
  private String name;

  /** Number of credits (used for calculating lesson blocks) */
  private int credits;

  /** Target year for this subject (1, 2, 3, etc.) */
  private int targetYear;

  /** Target semester (1 or 2) */
  private int targetSemester;

  private boolean fixedDaySession;

  /** If it's an optional subject it needs to be grouped with other options **/
  @Nullable
  private Long optionalGroupId;

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (!(o instanceof SubjectInfo))
      return false;
    SubjectInfo that = (SubjectInfo) o;
    return id != null && id.equals(that.id);
  }

  @Override
  public int hashCode() {
    return id != null ? id.hashCode() : 0;
  }

  @Override
  public String toString() {
    return "SubjectInfo{" + "id=" + id + ", name='" + name + '\'' + ", credits=" + credits + '}';
  }
}
