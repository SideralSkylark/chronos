package com.timetable.timetable.scheduler_engine.domain;

import java.util.Comparator;

/**
 * Comparator for determining the difficulty of scheduling a lesson.
 *
 * <p>More difficult lessons are scheduled FIRST in construction heuristics. This helps the solver
 * avoid getting stuck with impossible-to-schedule lessons at the end.
 *
 * <p>Difficulty criteria (in order of importance): 1. Student count (larger cohorts need specific
 * rooms) - DESCENDING 2. Subject credits (more blocks to schedule) - DESCENDING 3. Lesson ID (for
 * consistency) - ASCENDING
 */
public class LessonAssignmentDifficultyComparator implements Comparator<LessonAssignment> {

  @Override
  public int compare(LessonAssignment a, LessonAssignment b) {
    // Criterion 1: Student count (larger cohorts are harder)
    // Descending: larger values = more difficult = scheduled first
    int studentCountDiff = Integer.compare(b.getStudentCount(), a.getStudentCount());
    if (studentCountDiff != 0) {
      return studentCountDiff;
    }

    // Criterion 2: Subject credits (more blocks are harder to fit)
    // Descending: more credits = more difficult
    int aCohortBlocks =
        a.getCohortSubject() != null ? a.getCohortSubject().getLessonBlocksPerWeek() : 0;
    int bCohortBlocks =
        b.getCohortSubject() != null ? b.getCohortSubject().getLessonBlocksPerWeek() : 0;
    int blocksDiff = Integer.compare(bCohortBlocks, aCohortBlocks);
    if (blocksDiff != 0) {
      return blocksDiff;
    }

    // Criterion 3: Lesson ID (for stable sorting)
    // Ascending: lower IDs first
    return Long.compare(a.getId(), b.getId());
  }
}
