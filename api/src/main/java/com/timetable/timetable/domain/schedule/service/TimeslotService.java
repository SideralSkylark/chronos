package com.timetable.timetable.domain.schedule.service;

import com.timetable.timetable.domain.schedule.entity.Timeslot;
import com.timetable.timetable.domain.schedule.exception.TimeslotNotFoundException;
import com.timetable.timetable.domain.schedule.repository.TimeslotRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TimeslotService {
  private final TimeslotRepository timeslotRepository;

  public Timeslot getById(Long id) {
    log.debug("fetching timeslot {}", id);
    Timeslot timeslot =
        timeslotRepository
            .findById(id)
            .orElseThrow(() -> new TimeslotNotFoundException("no timeslot %d found".formatted(id)));

    log.info("timeslot {} found", id);
    return timeslot;
  }
}
