package com.timetable.timetable.scheduler_engine.domain;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.entity.PlanningPin;
import ai.timefold.solver.core.api.domain.lookup.PlanningId;
import ai.timefold.solver.core.api.domain.variable.PlanningVariable;
import com.timetable.timetable.scheduler_engine.domain.info.*;
import lombok.*;

/**
 * Represents a single lesson that needs to be scheduled. The solver will assign
 * a timeslot and room
 * to each lesson assignment.
 */
@PlanningEntity(comparatorClass = LessonAssignmentDifficultyComparator.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LessonAssignment {

  // ===== PLANNING ID =====
  // Unique identifier for this lesson assignment

  @PlanningId
  private Long id;

  // ===== PROBLEM FACTS =====
  // Fixed information about this lesson (doesn't change during solving)

  /**
   * The cohort-subject combination this lesson belongs to. Contains information
   * about the cohort,
   * subject, and assigned teacher.
   */
  private CohortSubjectInfo cohortSubject;

  /**
   * Which block number this is (e.g., block 1 of 5 for this cohort-subject). Used
   * for tracking and
   * reporting.
   */
  private int blockNumber;

  // ===== PLANNING VARIABLES =====
  // These will be assigned by the solver

  /**
   * The timeslot when this lesson will occur. Initially null, assigned by the
   * solver.
   */
  @PlanningVariable(valueRangeProviderRefs = "timeslotRange")
  private TimeslotInfo timeslot;

  /**
   * The room where this lesson will take place. Initially null, assigned by the
   * solver.
   */
  @PlanningVariable(valueRangeProviderRefs = "roomRange")
  private RoomInfo room;

  @PlanningPin
  private boolean pinned;

  // ===== CONVENIENCE METHODS =====
  // Quick access to nested information

  /** Gets the teacher assigned to this lesson */
  public TeacherInfo getTeacher() {
    return cohortSubject != null ? cohortSubject.getTeacher() : null;
  }

  public String getOptionalGroupId() {
    SubjectInfo subject = getSubject();
    if (subject == null || subject.getOptionalGroupId() == null) {
      return null;
    }
    CohortInfo cohort = getCohort();
    String cohortId = (cohort == null || cohort.getId() == null) ? "?" : cohort.getId().toString();
    return subject.getOptionalGroupId() + "-" + cohortId;
  }

  /** Gets the cohort (student group) for this lesson */
  public CohortInfo getCohort() {
    return cohortSubject != null ? cohortSubject.getCohort() : null;
  }

  /** Gets the subject being taught */
  public SubjectInfo getSubject() {
    return cohortSubject != null ? cohortSubject.getSubject() : null;
  }

  /** Gets the course ID (useful for room restrictions) */
  public Long getCourseId() {
    CohortInfo cohort = getCohort();
    return cohort != null ? cohort.getCourseId() : null;
  }

  /** Gets the number of students in this lesson */
  public int getStudentCount() {
    CohortInfo cohort = getCohort();
    return cohort != null ? cohort.getStudentCount() : 0;
  }

  /** Returns a human-readable description of this lesson */
  public String getDisplayName() {
    if (cohortSubject == null)
      return "Unknown Lesson";
    return cohortSubject.getDisplayName() + " [Block " + blockNumber + "]";
  }

  public boolean isSimulationTeam() {
    TeacherInfo t = getTeacher();
    return t != null && t.isSimulationTeam();
  }

  public boolean isFixedDaySession() {
    SubjectInfo s = getSubject();
    return s != null && s.isFixedDaySession();
  }

  /** Checks if this lesson is fully assigned (has both timeslot and room) */
  public boolean isAssigned() {
    return timeslot != null && room != null;
  }

  @Override
  public String toString() {
    return "LessonAssignment{"
        + "id="
        + id
        + ", cohortSubject="
        + (cohortSubject != null ? cohortSubject.getDisplayName() : "null")
        + ", block="
        + blockNumber
        + ", timeslot="
        + (timeslot != null ? timeslot.getDisplayName() : "null")
        + ", room="
        + (room != null ? room.getName() : "null")
        + '}';
  }
}
