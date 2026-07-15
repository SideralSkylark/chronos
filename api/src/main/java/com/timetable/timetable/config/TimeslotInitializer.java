package com.timetable.timetable.config;

import com.timetable.timetable.domain.schedule.entity.Timeslot;
import com.timetable.timetable.domain.schedule.repository.TimeslotRepository;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class TimeslotInitializer implements CommandLineRunner {

  private final TimeslotRepository timeslotRepository;

  private record Block(LocalTime start, LocalTime end) {}

  private static final Block[] MORNING_BLOCKS = {
    new Block(LocalTime.of(7, 0), LocalTime.of(8, 45)),
    new Block(LocalTime.of(8, 50), LocalTime.of(10, 35)),
    new Block(LocalTime.of(10, 40), LocalTime.of(12, 25))
  };

  private static final Block[] AFTERNOON_BLOCKS = {
    new Block(LocalTime.of(12, 30), LocalTime.of(14, 15)),
    new Block(LocalTime.of(14, 20), LocalTime.of(16, 5)),
    new Block(LocalTime.of(16, 10), LocalTime.of(17, 55))
  };

  private static final DayOfWeek[] WEEKDAYS = {
    DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY
  };

  @Override
  public void run(String... args) {
    if (timeslotRepository.count() > 0) {
      log.info("Timeslots already initialized, skipping.");
      return;
    }

    log.info("Initializing timeslots...");
    List<Timeslot> timeslots = new ArrayList<>();

    for (DayOfWeek day : WEEKDAYS) {
      for (Block block : MORNING_BLOCKS) {
        timeslots.add(buildTimeslot(day, block));
      }
      for (Block block : AFTERNOON_BLOCKS) {
        timeslots.add(buildTimeslot(day, block));
      }
    }

    timeslotRepository.saveAll(timeslots);
    log.info("Initialized {} timeslots (3 morning + 3 afternoon x 5 days)", timeslots.size());
  }

  private Timeslot buildTimeslot(DayOfWeek day, Block block) {
    return Timeslot.builder().dayOfWeek(day).startTime(block.start()).endTime(block.end()).build();
  }
}
