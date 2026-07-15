package com.timetable.timetable.scheduler_engine.preparation;

import com.timetable.timetable.domain.schedule.entity.*;
import com.timetable.timetable.domain.schedule.repository.*;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class FixedDaySessionService {

  private final ScheduledClassRepository scheduledClassRepository;
  private final TimeslotRepository timeslotRepository;
  private final RoomRepository roomRepository;

  @Transactional
  public int preAssign(List<CohortSubject> cohortSubjects, Timetable timetable) {

    List<CohortSubject> fixedOnes =
        cohortSubjects.stream()
            .filter(cs -> cs.getSubject().isFixedDaySession())
            .filter(cs -> cs.getSubject().getFixedDayOfWeek() != null)
            .toList();

    if (fixedOnes.isEmpty()) {
      log.debug("No fixed day sessions to pre-assign");
      return 0;
    }

    List<Timeslot> allTimeslots = timeslotRepository.findAll();
    List<Room> allRooms = roomRepository.findAll();
    int created = 0;

    for (CohortSubject cs : fixedOnes) {
      DayOfWeek fixedDay = cs.getSubject().getFixedDayOfWeek();
      int cohortYear = cs.getCohort().getYear();

      List<Timeslot> slotsForDay =
          allTimeslots.stream()
              .filter(ts -> ts.getDayOfWeek().equals(fixedDay))
              .filter(ts -> isPeriodAllowed(ts, cohortYear))
              .sorted(Comparator.comparing(Timeslot::getStartTime))
              .toList();

      int blocksNeeded = cs.getLessonBlocksPerWeek();

      if (slotsForDay.size() < blocksNeeded) {
        log.warn(
            "Not enough slots on {} for {} ({} needed, {} available)",
            fixedDay,
            cs.getSubject().getName(),
            blocksNeeded,
            slotsForDay.size());
        continue;
      }

      List<Timeslot> selectedSlots = slotsForDay.stream().limit(blocksNeeded).toList();

      for (Timeslot slot : selectedSlots) {
        boolean alreadyExists = scheduledClassRepository.existsByCohortSubjectAndTimeslot(cs, slot);
        if (alreadyExists) {
          log.debug(
              "Already pre-assigned: {} @ {}", cs.getSubject().getName(), slot.getDisplayName());
          continue;
        }

        // Encontra sala livre com capacidade suficiente
        Set<Long> occupiedRoomIds =
            scheduledClassRepository.findByTimetableAndTimeslot(timetable, slot).stream()
                .map(sc -> sc.getRoom().getId())
                .collect(Collectors.toSet());

        Room room =
            allRooms.stream()
                .filter(r -> !occupiedRoomIds.contains(r.getId()))
                .filter(r -> r.getCapacity() >= cs.getCohort().getStudentCount())
                .findFirst()
                .orElse(null);

        if (room == null) {
          log.warn(
              "No available room for {} @ {}", cs.getSubject().getName(), slot.getDisplayName());
          continue;
        }

        scheduledClassRepository.save(
            ScheduledClass.builder()
                .timetable(timetable)
                .cohortSubject(cs)
                .timeslot(slot)
                .room(room)
                .pinned(true)
                .build());

        created++;
        log.info(
            "Pre-assigned: {} / {} → {} @ {}",
            cs.getCohort().getDisplayName(),
            cs.getSubject().getName(),
            room.getName(),
            slot.getDisplayName());
      }
    }

    log.info("Fixed day pre-assignment complete: {} ScheduledClasses created", created);
    return created;
  }

  private boolean isPeriodAllowed(Timeslot ts, int cohortYear) {
    boolean isOddYear = cohortYear % 2 != 0;
    boolean isMorning = ts.getStartTime().isBefore(LocalTime.of(12, 0));
    return isOddYear == isMorning;
  }
}
