package com.timetable.timetable.sheduler_engine.solver;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.util.Optional;

import com.timetable.timetable.domain.schedule.entity.ScheduledClass;
import com.timetable.timetable.domain.schedule.entity.Timeslot;
import com.timetable.timetable.domain.schedule.repository.ScheduledClassRepository;
import com.timetable.timetable.scheduler_engine.solver.PermutationService;

import org.junit.Test;
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
    ScheduledClass class1 = new ScheduledClass();
    class1.setId(1L);
    class1.setTimeslot(timeslot1);

    Timeslot timeslot2 = new Timeslot();
    timeslot2.setId(2L);
    ScheduledClass class2 = new ScheduledClass();
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
}
