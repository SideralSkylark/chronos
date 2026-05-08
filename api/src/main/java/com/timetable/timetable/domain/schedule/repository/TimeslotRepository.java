package com.timetable.timetable.domain.schedule.repository;

import com.timetable.timetable.domain.schedule.entity.Timeslot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TimeslotRepository extends JpaRepository<Timeslot, Long> {
}
