package com.timetable.timetable.scheduler_engine.solver;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import com.timetable.timetable.domain.schedule.entity.CohortSubject;
import com.timetable.timetable.domain.schedule.entity.OptionalGroup;
import com.timetable.timetable.domain.schedule.entity.ScheduledClass;
import com.timetable.timetable.domain.schedule.entity.Subject;
import com.timetable.timetable.domain.schedule.entity.Timeslot;
import com.timetable.timetable.domain.schedule.entity.Timetable;
import com.timetable.timetable.domain.schedule.repository.ScheduledClassRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PremutationServiceTest {

  @Mock
  private ScheduledClassRepository scheduledClassRepository;

  @InjectMocks
  private PermutationService permutationService;

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
