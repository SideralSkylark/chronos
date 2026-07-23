package com.timetable.timetable.scheduler_engine.solver;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import ai.timefold.solver.core.api.score.buildin.hardsoft.HardSoftScore;
import ai.timefold.solver.core.api.solver.SolutionManager;
import ai.timefold.solver.core.api.solver.SolverFactory;
import ai.timefold.solver.core.config.solver.SolverConfig;

import com.timetable.timetable.domain.schedule.entity.Cohort;
import com.timetable.timetable.domain.schedule.entity.CohortSubject;
import com.timetable.timetable.domain.schedule.entity.ScheduledClass;
import com.timetable.timetable.domain.schedule.entity.Subject;
import com.timetable.timetable.domain.schedule.repository.RoomRepository;
import com.timetable.timetable.domain.schedule.repository.ScheduledClassRepository;
import com.timetable.timetable.domain.schedule.repository.TimeslotRepository;
import com.timetable.timetable.scheduler_engine.domain.LessonAssignment;
import com.timetable.timetable.scheduler_engine.domain.TimetableSolution;
import com.timetable.timetable.scheduler_engine.domain.info.*;
import com.timetable.timetable.scheduler_engine.mapper.TimetableSolutionMapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Tests findValidSlots against a REAL SolutionManager. Confirms behavior when
 * a candidate timeslot is already occupied by an optional-group pair from a
 * different cohort — the "exactly one occupant" assumption in
 * occupantsByTimeslotId breaks down, since a legitimate optional pair puts
 * two other-cohort lessons in the same slot.
 */
@ExtendWith(MockitoExtension.class)
class FindValidSlotsConstraintTest {

  @Mock
  private ScheduledClassRepository scheduledClassRepository;
  @Mock
  private TimeslotRepository timeslotRepository;
  @Mock
  private RoomRepository roomRepository;
  @Mock
  private TimetableSolutionMapper solutionMapper;

  private PermutationService permutationService;

  @BeforeEach
  void setUp() {
    SolverConfig solverConfig = new SolverConfig()
        .withSolutionClass(TimetableSolution.class)
        .withEntityClasses(LessonAssignment.class)
        .withConstraintProviderClass(TimetableConstraintProvider.class);

    SolverFactory<TimetableSolution> solverFactory = SolverFactory.create(solverConfig);
    SolutionManager<TimetableSolution, HardSoftScore> realSolutionManager = SolutionManager.create(solverFactory);

    permutationService = new PermutationService(
        scheduledClassRepository, timeslotRepository, roomRepository,
        solutionMapper, realSolutionManager);
  }

  /**
   * Candidate timeslot already hosts an optional-group pair from a different
   * cohort (two lessons, same slot — legitimate co-scheduling). Since
   * occupantsByTimeslotId finds 2 occupants there (not exactly 1), the
   * "occupant" swap-detection is skipped entirely, and — because placing the
   * target there introduces no actual room/teacher/cohort conflict — the slot
   * is reported as an "empty" candidate, even though two other lessons are
   * genuinely scheduled there. Documents this labeling gap; not a hard
   * constraint violation, just a UI-facing mischaracterization.
   */
  @Test
  void shouldReportBothOccupantsAsDisplaced_whenSlotHostsAnOptionalPair() {
    int academicYear = 2026;
    int semester = 1;

    Cohort cohortA = new Cohort();
    cohortA.setId(100L);
    cohortA.setYear(1);
    Cohort cohortB = new Cohort();
    cohortB.setId(200L);
    cohortB.setYear(1);

    Subject targetSubjectEntity = new Subject();
    targetSubjectEntity.setId(1L);
    targetSubjectEntity.setOptionalGroup(null);
    CohortSubject targetCohortSubject = new CohortSubject();
    targetCohortSubject.setCohort(cohortA);
    targetCohortSubject.setSubject(targetSubjectEntity);
    ScheduledClass targetSc = new ScheduledClass();
    targetSc.setId(1L);
    targetSc.setCohortSubject(targetCohortSubject);

    when(scheduledClassRepository.findAllWithDetailsByPeriod(academicYear, semester))
        .thenReturn(List.of(targetSc));
    when(timeslotRepository.findAll()).thenReturn(List.of());
    when(roomRepository.findAll()).thenReturn(List.of());

    TimeslotInfo timeslotOriginal = TimeslotInfo.builder()
        .id(1L).dayOfWeek(DayOfWeek.MONDAY)
        .startTime(LocalTime.of(8, 0)).endTime(LocalTime.of(9, 40)).build();
    TimeslotInfo timeslotCandidate = TimeslotInfo.builder()
        .id(2L).dayOfWeek(DayOfWeek.MONDAY)
        .startTime(LocalTime.of(10, 0)).endTime(LocalTime.of(11, 40)).build();

    RoomInfo roomTarget = RoomInfo.builder().id(10L).capacity(100).build();
    RoomInfo roomOccupant = RoomInfo.builder().id(11L).capacity(100).build();
    RoomInfo roomPair = RoomInfo.builder().id(12L).capacity(100).build();

    CohortInfo cohortInfoA = CohortInfo.builder()
        .id(100L).displayName("Cohort A").studentCount(30).courseId(1L).year(1).build();
    CohortInfo cohortInfoB = CohortInfo.builder()
        .id(200L).displayName("Cohort B").studentCount(30).courseId(2L).year(1).build();

    TeacherInfo teacherTarget = TeacherInfo.builder().id(1L).name("Teacher Target").build();
    TeacherInfo teacherOccupant = TeacherInfo.builder().id(2L).name("Teacher Occupant").build();
    TeacherInfo teacherPair = TeacherInfo.builder().id(3L).name("Teacher Pair").build();

    SubjectInfo targetSubjectInfo = SubjectInfo.builder().id(1L).name("Target").build();
    CohortSubjectInfo targetCsInfo = CohortSubjectInfo.builder()
        .id(1L).cohort(cohortInfoA).subject(targetSubjectInfo).teacher(teacherTarget).build();
    LessonAssignment targetLesson = LessonAssignment.builder()
        .id(1L).cohortSubject(targetCsInfo).blockNumber(1)
        .timeslot(timeslotOriginal).room(roomTarget).build();

    SubjectInfo occupantSubjectInfo = SubjectInfo.builder()
        .id(2L).name("Occupant Elective A").optionalGroupId(50L).build();
    CohortSubjectInfo occupantCsInfo = CohortSubjectInfo.builder()
        .id(2L).cohort(cohortInfoB).subject(occupantSubjectInfo).teacher(teacherOccupant).build();
    LessonAssignment occupantLesson = LessonAssignment.builder()
        .id(2L).cohortSubject(occupantCsInfo).blockNumber(1)
        .timeslot(timeslotCandidate).room(roomOccupant).build();

    SubjectInfo pairSubjectInfo = SubjectInfo.builder()
        .id(3L).name("Occupant Elective B").optionalGroupId(50L).build();
    CohortSubjectInfo pairCsInfo = CohortSubjectInfo.builder()
        .id(3L).cohort(cohortInfoB).subject(pairSubjectInfo).teacher(teacherPair).build();
    LessonAssignment pairLesson = LessonAssignment.builder()
        .id(3L).cohortSubject(pairCsInfo).blockNumber(1) // same block as occupant
        .timeslot(timeslotCandidate).room(roomPair).build(); // co-scheduled with occupant

    TimetableSolution solution = TimetableSolution.builder()
        .lessonAssignments(new ArrayList<>(List.of(targetLesson, occupantLesson, pairLesson)))
        .availableTimeslots(List.of(timeslotOriginal, timeslotCandidate))
        .availableRooms(List.of(roomTarget, roomOccupant, roomPair))
        .academicYear(academicYear)
        .semester(semester)
        .build();

    when(solutionMapper.fromScheduledClasses(any(), any(), any(), eq(academicYear), eq(semester)))
        .thenReturn(solution);

    List<PermutationService.ValidSlotResponse> result =
        permutationService.findValidSlots(1L, academicYear, semester);

    PermutationService.ValidSlotResponse candidateResponse = result.stream()
        .filter(r -> r.timeslotId() == timeslotCandidate.getId())
        .findFirst()
        .orElse(null);

    assertTrue(candidateResponse != null, "Expected timeslotCandidate to be reported");
    assertTrue(candidateResponse.isSwap(), "Expected timeslotCandidate to be reported as a swap, not empty");
    assertEquals(2, candidateResponse.displaced().size(), "Expected both occupants to be reported as displaced");
    assertTrue(
        candidateResponse.displaced().stream().anyMatch(d -> d.scheduledClassId().equals(2L)),
        "Expected occupant (id=2) in displaced list");
    assertTrue(
        candidateResponse.displaced().stream().anyMatch(d -> d.scheduledClassId().equals(3L)),
        "Expected occupant's pair (id=3) in displaced list");
  }
}
