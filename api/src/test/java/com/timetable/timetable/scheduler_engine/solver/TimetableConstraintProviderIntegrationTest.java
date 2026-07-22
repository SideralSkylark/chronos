package com.timetable.timetable.scheduler_engine.solver;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.timefold.solver.core.api.score.buildin.hardsoft.HardSoftScore;
import ai.timefold.solver.core.api.solver.SolutionManager;
import ai.timefold.solver.core.api.solver.SolverFactory;
import ai.timefold.solver.core.config.solver.SolverConfig;

import com.timetable.timetable.scheduler_engine.domain.LessonAssignment;
import com.timetable.timetable.scheduler_engine.domain.TimetableSolution;
import com.timetable.timetable.scheduler_engine.domain.info.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * Real Timefold constraint verification — no mocks, no fake scorer.
 * Confirms the actual ConstraintProvider flags the scenarios our
 * PermutationService pairing logic is meant to prevent.
 */
class TimetableConstraintProviderIntegrationTest {

  private SolutionManager<TimetableSolution, HardSoftScore> solutionManager;

  @BeforeEach
  void setUp() {
    SolverConfig solverConfig = new SolverConfig()
        .withSolutionClass(TimetableSolution.class)
        .withEntityClasses(LessonAssignment.class)
        .withConstraintProviderClass(TimetableConstraintProvider.class);

    SolverFactory<TimetableSolution> solverFactory = SolverFactory.create(solverConfig);
    solutionManager = SolutionManager.create(solverFactory);
  }

  @Test
  void shouldFlagHardViolation_whenOptionalPairIsSplitAcrossTimeslots() {
    CohortInfo cohort = CohortInfo.builder()
        .id(100L).displayName("Cohort C").studentCount(30).courseId(1L).year(1).build();
    TeacherInfo teacher = TeacherInfo.builder().id(1L).name("Teacher").build();

    TimeslotInfo timeslotA = TimeslotInfo.builder()
        .id(1L).dayOfWeek(java.time.DayOfWeek.MONDAY)
        .startTime(java.time.LocalTime.of(8, 0)).endTime(java.time.LocalTime.of(9, 40)).build();
    TimeslotInfo timeslotB = TimeslotInfo.builder()
        .id(2L).dayOfWeek(java.time.DayOfWeek.MONDAY)
        .startTime(java.time.LocalTime.of(10, 0)).endTime(java.time.LocalTime.of(11, 40)).build();

    RoomInfo roomX = RoomInfo.builder().id(10L).capacity(100).build();
    RoomInfo roomY = RoomInfo.builder().id(11L).capacity(100).build();

    SubjectInfo subjectA = SubjectInfo.builder().id(1L).name("Elective A").optionalGroupId(50L).build();
    CohortSubjectInfo csA = CohortSubjectInfo.builder()
        .id(1L).cohort(cohort).subject(subjectA).teacher(teacher).build();
    LessonAssignment lessonA = LessonAssignment.builder()
        .id(1L).cohortSubject(csA).blockNumber(1).timeslot(timeslotA).room(roomX).build();

    SubjectInfo subjectB = SubjectInfo.builder().id(2L).name("Elective B").optionalGroupId(50L).build();
    CohortSubjectInfo csB = CohortSubjectInfo.builder()
        .id(2L).cohort(cohort).subject(subjectB).teacher(teacher).build();
    // Same optional group, same block, but a DIFFERENT timeslot — this is
    // exactly what happens when a candidate's pair is left behind by a swap.
    LessonAssignment lessonB = LessonAssignment.builder()
        .id(2L).cohortSubject(csB).blockNumber(1).timeslot(timeslotB).room(roomY).build();

    TimetableSolution solution = TimetableSolution.builder()
        .lessonAssignments(new ArrayList<>(List.of(lessonA, lessonB)))
        .availableTimeslots(List.of(timeslotA, timeslotB))
        .availableRooms(List.of(roomX, roomY))
        .academicYear(2026)
        .semester(1)
        .build();

    solutionManager.update(solution);

    assertTrue(solution.getScore().hardScore() < 0,
        "Expected a hard violation when an optional pair is split across timeslots");
  }

  @Test
  void shouldScoreZero_whenOptionalPairSharesTimeslot() {
    CohortInfo cohort = CohortInfo.builder()
        .id(100L).displayName("Cohort C").studentCount(30).courseId(1L).year(1).build();
    TeacherInfo teacherA = TeacherInfo.builder().id(1L).name("Teacher A").build();
    TeacherInfo teacherB = TeacherInfo.builder().id(2L).name("Teacher B").build(); // different teacher

    TimeslotInfo timeslotA = TimeslotInfo.builder()
        .id(1L).dayOfWeek(java.time.DayOfWeek.MONDAY)
        .startTime(java.time.LocalTime.of(8, 0)).endTime(java.time.LocalTime.of(9, 40)).build();

    RoomInfo roomX = RoomInfo.builder().id(10L).capacity(100).build();
    RoomInfo roomY = RoomInfo.builder().id(11L).capacity(100).build();

    SubjectInfo subjectA = SubjectInfo.builder().id(1L).name("Elective A").optionalGroupId(50L).build();
    CohortSubjectInfo csA = CohortSubjectInfo.builder()
        .id(1L).cohort(cohort).subject(subjectA).teacher(teacherA).build();
    LessonAssignment lessonA = LessonAssignment.builder()
        .id(1L).cohortSubject(csA).blockNumber(1).timeslot(timeslotA).room(roomX).build();

    SubjectInfo subjectB = SubjectInfo.builder().id(2L).name("Elective B").optionalGroupId(50L).build();
    CohortSubjectInfo csB = CohortSubjectInfo.builder()
        .id(2L).cohort(cohort).subject(subjectB).teacher(teacherB).build(); // teacherB, not teacherA
    LessonAssignment lessonB = LessonAssignment.builder()
        .id(2L).cohortSubject(csB).blockNumber(1).timeslot(timeslotA).room(roomY).build();

    TimetableSolution solution = TimetableSolution.builder()
        .lessonAssignments(new ArrayList<>(List.of(lessonA, lessonB)))
        .availableTimeslots(List.of(timeslotA))
        .availableRooms(List.of(roomX, roomY))
        .academicYear(2026)
        .semester(1)
        .build();

    solutionManager.update(solution);

    assertEquals(0, solution.getScore().hardScore());
  }
}
