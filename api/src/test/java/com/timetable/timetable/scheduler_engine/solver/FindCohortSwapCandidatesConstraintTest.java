package com.timetable.timetable.scheduler_engine.solver;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.timetable.timetable.domain.schedule.entity.Cohort;
import com.timetable.timetable.domain.schedule.entity.CohortSubject;
import com.timetable.timetable.domain.schedule.entity.ScheduledClass;
import com.timetable.timetable.domain.schedule.entity.Subject;
import com.timetable.timetable.domain.schedule.repository.RoomRepository;
import com.timetable.timetable.domain.schedule.repository.ScheduledClassRepository;
import com.timetable.timetable.domain.schedule.repository.TimeslotRepository;
import com.timetable.timetable.scheduler_engine.domain.LessonAssignment;
import com.timetable.timetable.scheduler_engine.domain.TimetableSolution;
import com.timetable.timetable.scheduler_engine.domain.info.CohortInfo;
import com.timetable.timetable.scheduler_engine.domain.info.CohortSubjectInfo;
import com.timetable.timetable.scheduler_engine.domain.info.RoomInfo;
import com.timetable.timetable.scheduler_engine.domain.info.SubjectInfo;
import com.timetable.timetable.scheduler_engine.domain.info.TeacherInfo;
import com.timetable.timetable.scheduler_engine.domain.info.TimeslotInfo;
import com.timetable.timetable.scheduler_engine.mapper.TimetableSolutionMapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ai.timefold.solver.core.api.score.buildin.hardsoft.HardSoftScore;
import ai.timefold.solver.core.api.solver.SolutionManager;
import ai.timefold.solver.core.api.solver.SolverFactory;
import ai.timefold.solver.core.config.solver.SolverConfig;

@ExtendWith(MockitoExtension.class)
class FindCohortSwapCandidatesContraintTest {
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

    // Manual construction, not @InjectMocks: we need a REAL solutionManager
    // alongside mocked repositories and mapper.
    permutationService = new PermutationService(
        scheduledClassRepository, timeslotRepository, roomRepository,
        solutionMapper, realSolutionManager);
  }

  /**
   * Candidate belongs to an optional group; its pair is co-scheduled at
   * candidate's original timeslot. findCohortSwapCandidates now moves the
   * candidate's pair alongside it during tentative evaluation, so the
   * resulting state is a legitimate co-scheduling (candidate + its pair,
   * same group, same new timeslot) rather than a conflict — the candidate
   * correctly appears as valid.
   */
  @Test
  void shouldSurfaceCandidate_whenCandidatesOptionalPairIsMovedAlongWithIt() {
    int academicYear = 2026;
    int semester = 1;

    Cohort cohort = new Cohort();
    cohort.setId(100L);

    Subject targetSubjectEntity = new Subject();
    targetSubjectEntity.setId(1L);
    targetSubjectEntity.setOptionalGroup(null);
    CohortSubject targetCohortSubject = new CohortSubject();
    targetCohortSubject.setCohort(cohort);
    targetCohortSubject.setSubject(targetSubjectEntity);
    ScheduledClass targetSc = new ScheduledClass();
    targetSc.setId(1L);
    targetSc.setCohortSubject(targetCohortSubject);

    Subject candidateSubjectEntity = new Subject();
    candidateSubjectEntity.setId(2L);
    CohortSubject candidateCohortSubject = new CohortSubject();
    candidateCohortSubject.setCohort(cohort);
    candidateCohortSubject.setSubject(candidateSubjectEntity);
    ScheduledClass candidateSc = new ScheduledClass();
    candidateSc.setId(2L);
    candidateSc.setCohortSubject(candidateCohortSubject);

    Subject candidatePairSubjectEntity = new Subject();
    candidatePairSubjectEntity.setId(3L);
    CohortSubject candidatePairCohortSubject = new CohortSubject();
    candidatePairCohortSubject.setCohort(cohort);
    candidatePairCohortSubject.setSubject(candidatePairSubjectEntity);
    ScheduledClass candidatePairSc = new ScheduledClass();
    candidatePairSc.setId(3L);
    candidatePairSc.setCohortSubject(candidatePairCohortSubject);

    when(scheduledClassRepository.findAllWithDetailsByPeriod(academicYear, semester))
        .thenReturn(List.of(targetSc, candidateSc, candidatePairSc));
    when(timeslotRepository.findAll()).thenReturn(List.of());
    when(roomRepository.findAll()).thenReturn(List.of());

    // ── Solver-side fixtures ──
    TimeslotInfo timeslotA = TimeslotInfo.builder()
        .id(1L).dayOfWeek(DayOfWeek.MONDAY)
        .startTime(LocalTime.of(8, 0)).endTime(LocalTime.of(9, 40)).build();
    TimeslotInfo timeslotB = TimeslotInfo.builder()
        .id(2L).dayOfWeek(DayOfWeek.MONDAY)
        .startTime(LocalTime.of(10, 0)).endTime(LocalTime.of(11, 40)).build();

    RoomInfo roomX = RoomInfo.builder().id(10L).capacity(100).build();
    RoomInfo roomY = RoomInfo.builder().id(11L).capacity(100).build();
    RoomInfo roomZ = RoomInfo.builder().id(12L).capacity(100).build();

    CohortInfo cohortInfo = CohortInfo.builder()
        .id(100L).displayName("Cohort C").studentCount(30).courseId(1L).year(1).build();
    TeacherInfo teacherTarget = TeacherInfo.builder().id(1L).name("Teacher Target").build();
    TeacherInfo teacherCandidate = TeacherInfo.builder().id(2L).name("Teacher Candidate").build();
    TeacherInfo teacherPair = TeacherInfo.builder().id(3L).name("Teacher Pair").build();

    SubjectInfo targetSubjectInfo = SubjectInfo.builder().id(1L).name("Target").build();
    CohortSubjectInfo targetCsInfo = CohortSubjectInfo.builder()
        .id(1L).cohort(cohortInfo).subject(targetSubjectInfo).teacher(teacherTarget).build();
    LessonAssignment targetLesson = LessonAssignment.builder()
        .id(1L).cohortSubject(targetCsInfo).blockNumber(1)
        .timeslot(timeslotA).room(roomX).build();

    SubjectInfo candidateSubjectInfo = SubjectInfo.builder()
        .id(2L).name("Candidate").optionalGroupId(50L).build();
    CohortSubjectInfo candidateCsInfo = CohortSubjectInfo.builder()
        .id(2L).cohort(cohortInfo).subject(candidateSubjectInfo).teacher(teacherCandidate).build();
    LessonAssignment candidateLesson = LessonAssignment.builder()
        .id(2L).cohortSubject(candidateCsInfo).blockNumber(1)
        .timeslot(timeslotB).room(roomY).build();

    SubjectInfo candidatePairSubjectInfo = SubjectInfo.builder()
        .id(3L).name("Candidate Pair").optionalGroupId(50L).build();
    CohortSubjectInfo candidatePairCsInfo = CohortSubjectInfo.builder()
        .id(3L).cohort(cohortInfo).subject(candidatePairSubjectInfo).teacher(teacherPair).build();
    LessonAssignment candidatePairLesson = LessonAssignment.builder()
        .id(3L).cohortSubject(candidatePairCsInfo).blockNumber(1) // same block as candidate
        .timeslot(timeslotB).room(roomZ).build(); // legitimately co-scheduled with candidate

    TimetableSolution solution = TimetableSolution.builder()
        .lessonAssignments(new ArrayList<>(List.of(targetLesson, candidateLesson, candidatePairLesson)))
        .availableTimeslots(List.of(timeslotA, timeslotB))
        .availableRooms(List.of(roomX, roomY, roomZ))
        .academicYear(academicYear)
        .semester(semester)
        .build();

    when(solutionMapper.fromScheduledClasses(any(), any(), any(), eq(academicYear), eq(semester)))
        .thenReturn(solution);

    List<PermutationService.CohortSwapCandidate> result = permutationService.findCohortSwapCandidates(1L, academicYear,
        semester);

    assertTrue(result.stream().anyMatch(c -> c.scheduledClassId().equals(2L)));
  }

  @Test
  void findOptionalPair_shouldNotMatch_whenSameGroupButDifferentBlock() {
    CohortInfo cohortInfo = CohortInfo.builder().id(100L).displayName("Cohort C").build();
    TeacherInfo teacherInfo = TeacherInfo.builder().id(1L).name("Teacher").build();

    SubjectInfo subjectA = SubjectInfo.builder().id(1L).name("A").optionalGroupId(50L).build();
    CohortSubjectInfo csA = CohortSubjectInfo.builder()
        .id(1L).cohort(cohortInfo).subject(subjectA).teacher(teacherInfo).build();
    LessonAssignment lessonA = LessonAssignment.builder()
        .id(1L).cohortSubject(csA).blockNumber(1).build();

    SubjectInfo subjectOtherBlock = SubjectInfo.builder().id(2L).name("B").optionalGroupId(50L).build();
    CohortSubjectInfo csOtherBlock = CohortSubjectInfo.builder()
        .id(2L).cohort(cohortInfo).subject(subjectOtherBlock).teacher(teacherInfo).build();
    LessonAssignment otherBlockLesson = LessonAssignment.builder()
        .id(2L).cohortSubject(csOtherBlock).blockNumber(2).build(); // different block

    LessonAssignment result = permutationService.findOptionalPair(
        lessonA, List.of(lessonA, otherBlockLesson), lessonA.getId());

    assertNull(result);
  }

  @Test
  void findOptionalPair_shouldMatch_whenSameGroupAndSameBlock() {
    CohortInfo cohortInfo = CohortInfo.builder().id(100L).displayName("Cohort C").build();
    TeacherInfo teacherInfo = TeacherInfo.builder().id(1L).name("Teacher").build();

    SubjectInfo subjectA = SubjectInfo.builder().id(1L).name("A").optionalGroupId(50L).build();
    CohortSubjectInfo csA = CohortSubjectInfo.builder()
        .id(1L).cohort(cohortInfo).subject(subjectA).teacher(teacherInfo).build();
    LessonAssignment lessonA = LessonAssignment.builder()
        .id(1L).cohortSubject(csA).blockNumber(1).build();

    SubjectInfo subjectPair = SubjectInfo.builder().id(2L).name("Pair").optionalGroupId(50L).build();
    CohortSubjectInfo csPair = CohortSubjectInfo.builder()
        .id(2L).cohort(cohortInfo).subject(subjectPair).teacher(teacherInfo).build();
    LessonAssignment pairLesson = LessonAssignment.builder()
        .id(2L).cohortSubject(csPair).blockNumber(1).build();

    LessonAssignment result = permutationService.findOptionalPair(
        lessonA, List.of(lessonA, pairLesson), lessonA.getId());

    assertEquals(pairLesson, result);
  }
}
