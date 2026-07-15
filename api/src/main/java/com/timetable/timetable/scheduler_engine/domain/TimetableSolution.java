package com.timetable.timetable.scheduler_engine.domain;

import ai.timefold.solver.core.api.domain.solution.PlanningEntityCollectionProperty;
import ai.timefold.solver.core.api.domain.solution.PlanningScore;
import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.domain.solution.ProblemFactCollectionProperty;
import ai.timefold.solver.core.api.domain.valuerange.ValueRangeProvider;
import ai.timefold.solver.core.api.score.buildin.hardsoft.HardSoftScore;
import com.timetable.timetable.scheduler_engine.domain.info.RoomInfo;
import com.timetable.timetable.scheduler_engine.domain.info.TimeslotInfo;
import java.util.List;
import lombok.*;

/**
 * Represents the complete timetabling problem to be solved. Contains all lesson assignments
 * (planning entities) and available resources (problem facts).
 */
@PlanningSolution
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TimetableSolution {

  // ===== PLANNING ENTITIES =====
  // These are the objects the solver will assign timeslots and rooms to

  @PlanningEntityCollectionProperty private List<LessonAssignment> lessonAssignments;

  // ===== PROBLEM FACTS =====
  // These are the available resources that won't change during solving

  @ValueRangeProvider(id = "timeslotRange")
  @ProblemFactCollectionProperty
  private List<TimeslotInfo> availableTimeslots;

  @ValueRangeProvider(id = "roomRange")
  @ProblemFactCollectionProperty
  private List<RoomInfo> availableRooms;

  // ===== SCORE =====
  // Calculated by the constraint provider

  @PlanningScore private HardSoftScore score;

  // ===== METADATA =====
  // Context information about this timetable

  private int academicYear;
  private int semester;

  /** Returns a human-readable identifier for this timetable */
  public String getIdentifier() {
    return academicYear + "." + semester;
  }

  /** Returns the number of lessons that need to be scheduled */
  public int getTotalLessons() {
    return lessonAssignments != null ? lessonAssignments.size() : 0;
  }

  /** Returns the number of unassigned lessons (without timeslot or room) */
  public long getUnassignedLessons() {
    if (lessonAssignments == null) return 0;
    return lessonAssignments.stream()
        .filter(lesson -> lesson.getTimeslot() == null || lesson.getRoom() == null)
        .count();
  }

  /** Checks if the solution is feasible (all hard constraints satisfied) */
  public boolean isFeasible() {
    return score != null && score.hardScore() >= 0;
  }
}
