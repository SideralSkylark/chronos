package com.timetable.timetable.scheduler_engine.mapper;

import com.timetable.timetable.domain.schedule.entity.*;
import com.timetable.timetable.domain.schedule.repository.*;
import com.timetable.timetable.scheduler_engine.domain.*;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Handles persistence-level mapping with repository access. This mapper resolves entity references
 * when converting from solver domain back to JPA.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PersistenceMapper {

  // Inject your repositories here
  private final CohortSubjectRepository cohortSubjectRepository;
  private final TimeslotRepository timeslotRepository;
  private final RoomRepository roomRepository;

  /**
   * Converts a solved TimetableSolution to ScheduledClass entities with proper JPA references.
   *
   * <p>This method: 1. Iterates through all lesson assignments 2. Fetches the actual JPA entities
   * from the database using their IDs 3. Creates ScheduledClass entities linking everything
   * together 4. Associates them with the given Timetable
   *
   * @param solution The solved timetable from the solver
   * @param timetable The Timetable entity to link these classes to
   * @return List of ScheduledClass entities ready to be persisted
   * @throws IllegalStateException if any required entity cannot be found
   */
  public List<ScheduledClass> convertToScheduledClasses(
      TimetableSolution solution, Timetable timetable) {

    if (solution == null || solution.getLessonAssignments() == null) {
      log.warn("Cannot convert null solution");
      return new ArrayList<>();
    }

    log.info(
        "Converting {} lesson assignments to ScheduledClass entities with DB references",
        solution.getLessonAssignments().size());

    List<ScheduledClass> scheduledClasses = new ArrayList<>();
    int skippedCount = 0;

    for (LessonAssignment lesson : solution.getLessonAssignments()) {
      if (lesson.isPinned()) continue;
      // Skip unassigned lessons
      if (lesson.getTimeslot() == null || lesson.getRoom() == null) {
        log.warn(
            "Skipping unassigned lesson: {} (block {})",
            lesson.getCohortSubject().getDisplayName(),
            lesson.getBlockNumber());
        skippedCount++;
        continue;
      }

      try {
        // Fetch JPA entities by ID
        Long cohortSubjectId = lesson.getCohortSubject().getId();
        Long timeslotId = lesson.getTimeslot().getId();
        Long roomId = lesson.getRoom().getId();

        CohortSubject cohortSubject =
            cohortSubjectRepository
                .findById(cohortSubjectId)
                .orElseThrow(
                    () -> new IllegalStateException("CohortSubject not found: " + cohortSubjectId));

        Timeslot timeslot =
            timeslotRepository
                .findById(timeslotId)
                .orElseThrow(() -> new IllegalStateException("Timeslot not found: " + timeslotId));

        Room room =
            roomRepository
                .findById(roomId)
                .orElseThrow(() -> new IllegalStateException("Room not found: " + roomId));

        // Create the scheduled class
        ScheduledClass scheduledClass =
            ScheduledClass.builder()
                .cohortSubject(cohortSubject)
                .timeslot(timeslot)
                .room(room)
                .timetable(timetable)
                .build();

        scheduledClasses.add(scheduledClass);

      } catch (Exception e) {
        log.error(
            "Error converting lesson {} to ScheduledClass: {}", lesson.getId(), e.getMessage(), e);
        throw new IllegalStateException(
            "Failed to convert lesson assignment to scheduled class", e);
      }
    }

    log.info(
        "Successfully converted {} lessons to ScheduledClass entities ({} skipped as unassigned)",
        scheduledClasses.size(),
        skippedCount);

    if (skippedCount > 0) {
      log.warn(
          "WARNING: {} lessons were not assigned by the solver. "
              + "This may indicate infeasible constraints or insufficient resources.",
          skippedCount);
    }

    return scheduledClasses;
  }

  /**
   * Validates that a TimetableSolution is ready to be persisted.
   *
   * @param solution The solution to validate
   * @return true if all lessons are assigned, false otherwise
   */
  public boolean isSolutionComplete(TimetableSolution solution) {
    if (solution == null || solution.getLessonAssignments() == null) {
      return false;
    }

    long unassignedCount = solution.getUnassignedLessons();

    if (unassignedCount > 0) {
      log.warn("Solution is incomplete: {} lessons remain unassigned", unassignedCount);
      return false;
    }

    return true;
  }

  /** Gets statistics about a solution for logging/debugging. */
  public String getSolutionStats(TimetableSolution solution) {
    if (solution == null) return "null solution";

    return String.format(
        "TimetableSolution[%s.%d] - Total: %d, Unassigned: %d, Score: %s, Feasible: %s",
        solution.getAcademicYear(),
        solution.getSemester(),
        solution.getTotalLessons(),
        solution.getUnassignedLessons(),
        solution.getScore(),
        solution.isFeasible());
  }
}
