package com.timetable.timetable.scheduler_engine.solver;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import com.timetable.timetable.domain.schedule.entity.CohortSubject;
import com.timetable.timetable.domain.schedule.entity.OptionalGroup;
import com.timetable.timetable.domain.schedule.entity.Room;
import com.timetable.timetable.domain.schedule.entity.ScheduledClass;
import com.timetable.timetable.domain.schedule.entity.Subject;
import com.timetable.timetable.domain.schedule.entity.Timeslot;
import com.timetable.timetable.domain.schedule.entity.Timetable;
import com.timetable.timetable.domain.schedule.repository.RoomRepository;
import com.timetable.timetable.domain.schedule.repository.ScheduledClassRepository;
import com.timetable.timetable.domain.schedule.repository.TimeslotRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PremutationServiceTest {

  @Mock
  private ScheduledClassRepository scheduledClassRepository;

  @Mock
  private TimeslotRepository timeslotRepository;

  @Mock
  private RoomRepository roomRepository;

  @InjectMocks
  private PermutationService permutationService;

  @Test
  void shouldMoveScheduledClass_whenNoSwapAndNoOptionalGroup() {
    Timeslot originalTimeslot = new Timeslot();
    originalTimeslot.setId(1L);
    Timeslot newTimeslot = new Timeslot();
    newTimeslot.setId(2L);
    Room newRoom = new Room();
    newRoom.setId(10L);

    Subject subject = new Subject();
    subject.setOptionalGroup(null);
    CohortSubject cohortSubject = new CohortSubject();
    cohortSubject.setSubject(subject);

    ScheduledClass scX = new ScheduledClass();
    scX.setId(1L);
    scX.setCohortSubject(cohortSubject);
    scX.setTimeslot(originalTimeslot);

    when(scheduledClassRepository.findById(1L)).thenReturn(Optional.of(scX));
    when(timeslotRepository.findById(2L)).thenReturn(Optional.of(newTimeslot));
    when(roomRepository.findById(10L)).thenReturn(Optional.of(newRoom));

    permutationService.applySwap(1L, 2L, 10L, null);

    assertEquals(newTimeslot, scX.getTimeslot());
    assertEquals(newRoom, scX.getRoom());
  }

  /**
   * Move only (no swapWithId). X belongs to a group; its pair shares X's
   * original timeslot and should follow X to the new timeslot.
   */
  @Test
  void shouldMoveOptionalPair_whenMovingWithoutSwap() {
    Timetable timetable = new Timetable();
    timetable.setAcademicYear(2026);
    timetable.setSemester(1);

    Timeslot originalTimeslot = new Timeslot();
    originalTimeslot.setId(1L);
    Timeslot newTimeslot = new Timeslot();
    newTimeslot.setId(2L);
    Room newRoom = new Room();
    newRoom.setId(10L);

    OptionalGroup group = new OptionalGroup();
    group.setId(1L);

    Subject subjectX = new Subject();
    subjectX.setOptionalGroup(group);
    CohortSubject cohortSubjectX = new CohortSubject();
    cohortSubjectX.setSubject(subjectX);

    ScheduledClass scX = new ScheduledClass();
    scX.setId(1L);
    scX.setTimetable(timetable);
    scX.setCohortSubject(cohortSubjectX);
    scX.setTimeslot(originalTimeslot);

    Subject subjectPair = new Subject();
    subjectPair.setOptionalGroup(group);
    CohortSubject cohortSubjectPair = new CohortSubject();
    cohortSubjectPair.setSubject(subjectPair);

    ScheduledClass pairX = new ScheduledClass();
    pairX.setId(2L);
    pairX.setTimetable(timetable);
    pairX.setCohortSubject(cohortSubjectPair);
    pairX.setTimeslot(originalTimeslot); // same block as scX

    when(scheduledClassRepository.findById(1L)).thenReturn(Optional.of(scX));
    when(timeslotRepository.findById(2L)).thenReturn(Optional.of(newTimeslot));
    when(roomRepository.findById(10L)).thenReturn(Optional.of(newRoom));
    when(scheduledClassRepository.findAllWithDetailsByPeriod(2026, 1))
        .thenReturn(List.of(scX, pairX));

    permutationService.applySwap(1L, 2L, 10L, null);

    assertEquals(newTimeslot, scX.getTimeslot());
    assertEquals(newTimeslot, pairX.getTimeslot());
    verify(scheduledClassRepository).save(pairX);
  }

  /**
   * X's pair belongs to the same group but a DIFFERENT original block
   * (different timeslot) — it must NOT move.
   */
  @Test
  void shouldNotMoveOtherBlock_whenSameGroupButDifferentOriginalTimeslot() {
    Timetable timetable = new Timetable();
    timetable.setAcademicYear(2026);
    timetable.setSemester(1);

    Timeslot originalTimeslot = new Timeslot();
    originalTimeslot.setId(1L);
    Timeslot newTimeslot = new Timeslot();
    newTimeslot.setId(2L);
    Timeslot unrelatedTimeslot = new Timeslot();
    unrelatedTimeslot.setId(3L);
    Room newRoom = new Room();
    newRoom.setId(10L);

    OptionalGroup group = new OptionalGroup();
    group.setId(1L);

    Subject subjectX = new Subject();
    subjectX.setOptionalGroup(group);
    CohortSubject cohortSubjectX = new CohortSubject();
    cohortSubjectX.setSubject(subjectX);

    ScheduledClass scX = new ScheduledClass();
    scX.setId(1L);
    scX.setTimetable(timetable);
    scX.setCohortSubject(cohortSubjectX);
    scX.setTimeslot(originalTimeslot);

    Subject subjectOtherBlock = new Subject();
    subjectOtherBlock.setOptionalGroup(group); // same group
    CohortSubject cohortSubjectOtherBlock = new CohortSubject();
    cohortSubjectOtherBlock.setSubject(subjectOtherBlock);

    ScheduledClass otherBlock = new ScheduledClass();
    otherBlock.setId(2L);
    otherBlock.setTimetable(timetable);
    otherBlock.setCohortSubject(cohortSubjectOtherBlock);
    otherBlock.setTimeslot(unrelatedTimeslot); // different block, not scX's original slot

    when(scheduledClassRepository.findById(1L)).thenReturn(Optional.of(scX));
    when(timeslotRepository.findById(2L)).thenReturn(Optional.of(newTimeslot));
    when(roomRepository.findById(10L)).thenReturn(Optional.of(newRoom));
    when(scheduledClassRepository.findAllWithDetailsByPeriod(2026, 1))
        .thenReturn(List.of(scX, otherBlock));

    permutationService.applySwap(1L, 2L, 10L, null);

    assertEquals(unrelatedTimeslot, otherBlock.getTimeslot()); // untouched
    verify(scheduledClassRepository, never()).save(otherBlock);
  }

  /**
   * Full swap. X belongs to a group, Y is plain. X's pair should follow X
   * to X's destination (newTimeslot).
   */
  @Test
  void shouldMoveOptionalPair_whenFullSwapAndXBelongsToGroup() {
    Timetable timetable = new Timetable();
    timetable.setAcademicYear(2026);
    timetable.setSemester(1);

    Timeslot xOriginal = new Timeslot();
    xOriginal.setId(1L);
    Timeslot xNew = new Timeslot();
    xNew.setId(2L);
    Timeslot yOriginal = new Timeslot();
    yOriginal.setId(3L);
    Room newRoom = new Room();
    newRoom.setId(10L);

    OptionalGroup group = new OptionalGroup();
    group.setId(1L);

    Subject subjectX = new Subject();
    subjectX.setOptionalGroup(group);
    CohortSubject cohortSubjectX = new CohortSubject();
    cohortSubjectX.setSubject(subjectX);
    ScheduledClass scX = new ScheduledClass();
    scX.setId(1L);
    scX.setTimetable(timetable);
    scX.setCohortSubject(cohortSubjectX);
    scX.setTimeslot(xOriginal);

    Subject subjectY = new Subject();
    subjectY.setOptionalGroup(null);
    CohortSubject cohortSubjectY = new CohortSubject();
    cohortSubjectY.setSubject(subjectY);
    ScheduledClass scY = new ScheduledClass();
    scY.setId(2L);
    scY.setTimetable(timetable);
    scY.setCohortSubject(cohortSubjectY);
    scY.setTimeslot(yOriginal);

    Subject subjectPairX = new Subject();
    subjectPairX.setOptionalGroup(group);
    CohortSubject cohortSubjectPairX = new CohortSubject();
    cohortSubjectPairX.setSubject(subjectPairX);
    ScheduledClass pairX = new ScheduledClass();
    pairX.setId(3L);
    pairX.setTimetable(timetable);
    pairX.setCohortSubject(cohortSubjectPairX);
    pairX.setTimeslot(xOriginal); // shares X's original block

    when(scheduledClassRepository.findById(1L)).thenReturn(Optional.of(scX));
    when(scheduledClassRepository.findById(2L)).thenReturn(Optional.of(scY));
    when(timeslotRepository.findById(2L)).thenReturn(Optional.of(xNew));
    when(roomRepository.findById(10L)).thenReturn(Optional.of(newRoom));
    when(scheduledClassRepository.findAllWithDetailsByPeriod(2026, 1))
        .thenReturn(List.of(scX, scY, pairX));

    permutationService.applySwap(1L, 2L, 10L, 2L);

    assertEquals(xNew, scX.getTimeslot());
    assertEquals(xOriginal, scY.getTimeslot());
    assertEquals(xNew, pairX.getTimeslot());
    verify(scheduledClassRepository).save(pairX);
  }

  /**
   * Full swap. Only Y belongs to a group. Y's pair should follow Y to Y's
   * destination (xOriginal — the slot Y ends up in).
   */
  @Test
  void shouldMoveOptionalPair_whenFullSwapAndOnlyYBelongsToGroup() {
    Timetable timetable = new Timetable();
    timetable.setAcademicYear(2026);
    timetable.setSemester(1);

    Timeslot xOriginal = new Timeslot();
    xOriginal.setId(1L);
    Timeslot xNew = new Timeslot();
    xNew.setId(2L);
    Timeslot yOriginal = new Timeslot();
    yOriginal.setId(3L);
    Room newRoom = new Room();
    newRoom.setId(10L);

    OptionalGroup group = new OptionalGroup();
    group.setId(1L);

    Subject subjectX = new Subject();
    subjectX.setOptionalGroup(null);
    CohortSubject cohortSubjectX = new CohortSubject();
    cohortSubjectX.setSubject(subjectX);
    ScheduledClass scX = new ScheduledClass();
    scX.setId(1L);
    scX.setTimetable(timetable);
    scX.setCohortSubject(cohortSubjectX);
    scX.setTimeslot(xOriginal);

    Subject subjectY = new Subject();
    subjectY.setOptionalGroup(group);
    CohortSubject cohortSubjectY = new CohortSubject();
    cohortSubjectY.setSubject(subjectY);
    ScheduledClass scY = new ScheduledClass();
    scY.setId(2L);
    scY.setTimetable(timetable);
    scY.setCohortSubject(cohortSubjectY);
    scY.setTimeslot(yOriginal);

    Subject subjectPairY = new Subject();
    subjectPairY.setOptionalGroup(group);
    CohortSubject cohortSubjectPairY = new CohortSubject();
    cohortSubjectPairY.setSubject(subjectPairY);
    ScheduledClass pairY = new ScheduledClass();
    pairY.setId(3L);
    pairY.setTimetable(timetable);
    pairY.setCohortSubject(cohortSubjectPairY);
    pairY.setTimeslot(yOriginal); // shares Y's original block

    when(scheduledClassRepository.findById(1L)).thenReturn(Optional.of(scX));
    when(scheduledClassRepository.findById(2L)).thenReturn(Optional.of(scY));
    when(timeslotRepository.findById(2L)).thenReturn(Optional.of(xNew));
    when(roomRepository.findById(10L)).thenReturn(Optional.of(newRoom));
    when(scheduledClassRepository.findAllWithDetailsByPeriod(2026, 1))
        .thenReturn(List.of(scX, scY, pairY));

    permutationService.applySwap(1L, 2L, 10L, 2L);

    assertEquals(xNew, scX.getTimeslot());
    assertEquals(xOriginal, scY.getTimeslot());
    assertEquals(xOriginal, pairY.getTimeslot()); // pair should follow Y to Y's new slot
    verify(scheduledClassRepository).save(pairY);
  }

  /**
   * Full swap. Both X and Y belong to (different) groups — both pairs
   * should move.
   */
  @Test
  void shouldMoveBothOptionalPairs_whenFullSwapAndBothBelongToGroups() {
    Timetable timetable = new Timetable();
    timetable.setAcademicYear(2026);
    timetable.setSemester(1);

    Timeslot xOriginal = new Timeslot();
    xOriginal.setId(1L);
    Timeslot xNew = new Timeslot();
    xNew.setId(2L);
    Timeslot yOriginal = new Timeslot();
    yOriginal.setId(3L);
    Room newRoom = new Room();
    newRoom.setId(10L);

    OptionalGroup groupX = new OptionalGroup();
    groupX.setId(1L);
    OptionalGroup groupY = new OptionalGroup();
    groupY.setId(2L);

    Subject subjectX = new Subject();
    subjectX.setOptionalGroup(groupX);
    CohortSubject cohortSubjectX = new CohortSubject();
    cohortSubjectX.setSubject(subjectX);
    ScheduledClass scX = new ScheduledClass();
    scX.setId(1L);
    scX.setTimetable(timetable);
    scX.setCohortSubject(cohortSubjectX);
    scX.setTimeslot(xOriginal);

    Subject subjectY = new Subject();
    subjectY.setOptionalGroup(groupY);
    CohortSubject cohortSubjectY = new CohortSubject();
    cohortSubjectY.setSubject(subjectY);
    ScheduledClass scY = new ScheduledClass();
    scY.setId(2L);
    scY.setTimetable(timetable);
    scY.setCohortSubject(cohortSubjectY);
    scY.setTimeslot(yOriginal);

    Subject subjectPairX = new Subject();
    subjectPairX.setOptionalGroup(groupX);
    CohortSubject cohortSubjectPairX = new CohortSubject();
    cohortSubjectPairX.setSubject(subjectPairX);
    ScheduledClass pairX = new ScheduledClass();
    pairX.setId(3L);
    pairX.setTimetable(timetable);
    pairX.setCohortSubject(cohortSubjectPairX);
    pairX.setTimeslot(xOriginal);

    Subject subjectPairY = new Subject();
    subjectPairY.setOptionalGroup(groupY);
    CohortSubject cohortSubjectPairY = new CohortSubject();
    cohortSubjectPairY.setSubject(subjectPairY);
    ScheduledClass pairY = new ScheduledClass();
    pairY.setId(4L);
    pairY.setTimetable(timetable);
    pairY.setCohortSubject(cohortSubjectPairY);
    pairY.setTimeslot(yOriginal);

    when(scheduledClassRepository.findById(1L)).thenReturn(Optional.of(scX));
    when(scheduledClassRepository.findById(2L)).thenReturn(Optional.of(scY));
    when(timeslotRepository.findById(2L)).thenReturn(Optional.of(xNew));
    when(roomRepository.findById(10L)).thenReturn(Optional.of(newRoom));
    when(scheduledClassRepository.findAllWithDetailsByPeriod(2026, 1))
        .thenReturn(List.of(scX, scY, pairX, pairY));

    permutationService.applySwap(1L, 2L, 10L, 2L);

    assertEquals(xNew, scX.getTimeslot());
    assertEquals(xOriginal, scY.getTimeslot());
    assertEquals(xNew, pairX.getTimeslot());
    assertEquals(xOriginal, pairY.getTimeslot());
    verify(scheduledClassRepository).save(pairX);
    verify(scheduledClassRepository).save(pairY);
  }

  /**
   * Swap {@code ScheduledClass} A for B inside a cohort
   */
  @Test
  void shouldSwapTimeslotsBetweenScheduledClasses() {
    Timeslot timeslot1 = new Timeslot();
    timeslot1.setId(1L);
    Timeslot timeslot2 = new Timeslot();
    timeslot2.setId(2L);

    CohortSubject cohortSubject1 = new CohortSubject();
    cohortSubject1.setSubject(new Subject());
    ScheduledClass class1 = new ScheduledClass();
    class1.setCohortSubject(cohortSubject1);
    class1.setId(1L);
    class1.setTimeslot(timeslot1);

    CohortSubject cohortSubject2 = new CohortSubject();
    cohortSubject2.setSubject(new Subject());
    ScheduledClass class2 = new ScheduledClass();
    class2.setCohortSubject(cohortSubject2);
    class2.setId(2L);
    class2.setTimeslot(timeslot2);

    when(scheduledClassRepository.findById(1L))
        .thenReturn(Optional.of(class1));

    when(scheduledClassRepository.findById(2L))
        .thenReturn(Optional.of(class2));

    permutationService.applyCohortSwap(class1.getId(), class2.getId());

    assertEquals(timeslot1, class2.getTimeslot());
    assertEquals(timeslot2, class1.getTimeslot());
  }

  /**
   * when swaping {@code ScheduledClass} A for B inside a cohort,
   * if A is part of a group then move the pair together
   */
  @Test
  void shouldMoveOptionalPair_whenApplyingCohortSwap() {
    Timetable timetable = new Timetable();
    timetable.setAcademicYear(2026);
    timetable.setSemester(1);

    ScheduledClass scA = new ScheduledClass();
    scA.setId(1L);
    scA.setTimetable(timetable);
    ScheduledClass scB = new ScheduledClass();
    scB.setId(2L);
    scB.setTimetable(timetable);

    Timeslot timeslot1 = new Timeslot();
    timeslot1.setId(1L);
    Timeslot timeslot2 = new Timeslot();
    timeslot2.setId(2L);

    OptionalGroup optionalGroup = new OptionalGroup();
    optionalGroup.setId(1L);

    Subject subject1 = new Subject();
    subject1.setId(1L);
    subject1.setOptionalGroup(optionalGroup);
    CohortSubject cohortSubject1 = new CohortSubject();
    cohortSubject1.setId(1L);
    cohortSubject1.setSubject(subject1);
    scA.setCohortSubject(cohortSubject1);
    scA.setTimeslot(timeslot1);

    Subject subject2 = new Subject();
    subject2.setId(2L);
    subject2.setOptionalGroup(null);
    CohortSubject cohortSubject2 = new CohortSubject();
    cohortSubject2.setId(2L);
    cohortSubject2.setSubject(subject2);
    scB.setCohortSubject(cohortSubject2);
    scB.setTimeslot(timeslot2);

    ScheduledClass scAPair = new ScheduledClass();
    scAPair.setId(3L);
    scAPair.setTimetable(timetable);
    Subject subject3 = new Subject();
    subject3.setOptionalGroup(optionalGroup);
    CohortSubject cohortSubject3 = new CohortSubject();
    cohortSubject3.setSubject(subject3);
    scAPair.setCohortSubject(cohortSubject3);
    scAPair.setTimeslot(timeslot1);

    when(scheduledClassRepository.findById(1L))
        .thenReturn(Optional.of(scA));
    when(scheduledClassRepository.findById(2L))
        .thenReturn(Optional.of(scB));
    when(scheduledClassRepository.findAllWithDetailsByPeriod(2026, 1))
        .thenReturn(List.of(scA, scB, scAPair));

    permutationService.applyCohortSwap(1L, 2L);

    assertEquals(timeslot2, scA.getTimeslot());
    assertEquals(timeslot1, scB.getTimeslot());
    assertEquals(scA.getTimeslot(), scAPair.getTimeslot());
    verify(scheduledClassRepository).save(scAPair);
  }

  /**
   * when swaping {@code ScheduledClass} A for B inside a cohort,
   * if B is part of a group then move the pair together
   */
  @Test
  void shouldMoveOptionalPair_whenOnlyBBelognsToAGroup() {
    Timetable timetable = new Timetable();
    timetable.setAcademicYear(2026);
    timetable.setSemester(1);

    Timeslot timeslotA = new Timeslot();
    timeslotA.setId(1L);
    Timeslot timeslotB = new Timeslot();
    timeslotB.setId(2L);

    ScheduledClass scA = new ScheduledClass();
    scA.setId(1L);
    scA.setTimeslot(timeslotA);
    scA.setTimetable(timetable);
    ScheduledClass scB = new ScheduledClass();
    scB.setId(2L);
    scB.setTimeslot(timeslotB);
    scB.setTimetable(timetable);

    OptionalGroup optionalGroup = new OptionalGroup();
    optionalGroup.setId(1L);

    Subject subjectA = new Subject();
    subjectA.setOptionalGroup(null);
    CohortSubject cohortSubjectA = new CohortSubject();
    cohortSubjectA.setSubject(subjectA);
    scA.setCohortSubject(cohortSubjectA);

    Subject subjectB = new Subject();
    subjectB.setOptionalGroup(optionalGroup);
    CohortSubject cohortSubjectB = new CohortSubject();
    cohortSubjectB.setSubject(subjectB);
    scB.setCohortSubject(cohortSubjectB);

    ScheduledClass scBPair = new ScheduledClass();
    scBPair.setId(3L);
    Subject subjectC = new Subject();
    subjectC.setOptionalGroup(optionalGroup);
    CohortSubject cohortSubjectC = new CohortSubject();
    cohortSubjectC.setSubject(subjectC);
    scBPair.setCohortSubject(cohortSubjectC);
    scBPair.setTimetable(timetable);
    scBPair.setTimeslot(timeslotB);

    when(scheduledClassRepository.findById(1L))
        .thenReturn(Optional.of(scA));
    when(scheduledClassRepository.findById(2L))
        .thenReturn(Optional.of(scB));
    when(scheduledClassRepository.findAllWithDetailsByPeriod(2026, 1))
        .thenReturn(List.of(scA, scB, scBPair));

    permutationService.applyCohortSwap(scA.getId(), scB.getId());

    assertEquals(timeslotB, scA.getTimeslot());
    assertEquals(timeslotA, scB.getTimeslot());
    assertEquals(timeslotA, scBPair.getTimeslot());
    verify(scheduledClassRepository).save(scBPair);
  }

  /**
   * when swaping {@code ScheduledClass} A for B inside a cohort,
   * if Both are part of a group then move the pairs together
   */
  @Test
  void shouldMoveBothOptionalPairs_whenBothBelongToGroups() {
    Timetable timetable = new Timetable();
    timetable.setAcademicYear(2026);
    timetable.setSemester(1);

    Timeslot timeslot1 = new Timeslot();
    timeslot1.setId(1L);
    Timeslot timeslot2 = new Timeslot();
    timeslot2.setId(2L);

    OptionalGroup groupA = new OptionalGroup();
    groupA.setId(1L);
    OptionalGroup groupB = new OptionalGroup();
    groupB.setId(2L);

    Subject subjectA = new Subject();
    subjectA.setId(1L);
    subjectA.setOptionalGroup(groupA);
    CohortSubject cohortSubjectA = new CohortSubject();
    cohortSubjectA.setSubject(subjectA);
    ScheduledClass scA = new ScheduledClass();
    scA.setId(1L);
    scA.setTimetable(timetable);
    scA.setCohortSubject(cohortSubjectA);
    scA.setTimeslot(timeslot1);

    Subject subjectB = new Subject();
    subjectB.setId(2L);
    subjectB.setOptionalGroup(groupB);
    CohortSubject cohortSubjectB = new CohortSubject();
    cohortSubjectB.setSubject(subjectB);
    ScheduledClass scB = new ScheduledClass();
    scB.setId(2L);
    scB.setTimetable(timetable);
    scB.setCohortSubject(cohortSubjectB);
    scB.setTimeslot(timeslot2);

    Subject subjectAPair = new Subject();
    subjectAPair.setOptionalGroup(groupA);
    CohortSubject cohortSubjectAPair = new CohortSubject();
    cohortSubjectAPair.setSubject(subjectAPair);
    ScheduledClass scAPair = new ScheduledClass();
    scAPair.setId(3L);
    scAPair.setTimetable(timetable);
    scAPair.setCohortSubject(cohortSubjectAPair);
    scAPair.setTimeslot(timeslot1);

    Subject subjectBPair = new Subject();
    subjectBPair.setOptionalGroup(groupB);
    CohortSubject cohortSubjectBPair = new CohortSubject();
    cohortSubjectBPair.setSubject(subjectBPair);
    ScheduledClass scBPair = new ScheduledClass();
    scBPair.setId(4L);
    scBPair.setTimetable(timetable);
    scBPair.setCohortSubject(cohortSubjectBPair);
    scBPair.setTimeslot(timeslot2);

    when(scheduledClassRepository.findById(1L)).thenReturn(Optional.of(scA));
    when(scheduledClassRepository.findById(2L)).thenReturn(Optional.of(scB));
    when(scheduledClassRepository.findAllWithDetailsByPeriod(2026, 1))
        .thenReturn(List.of(scA, scB, scAPair, scBPair));

    permutationService.applyCohortSwap(1L, 2L);

    assertEquals(timeslot2, scA.getTimeslot());
    assertEquals(timeslot1, scB.getTimeslot());
    assertEquals(timeslot2, scAPair.getTimeslot());
    assertEquals(timeslot1, scBPair.getTimeslot());
    verify(scheduledClassRepository).save(scAPair);
    verify(scheduledClassRepository).save(scBPair);
  }
}
