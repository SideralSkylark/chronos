package com.timetable.timetable.domain.schedule.repository;

import com.timetable.timetable.domain.schedule.entity.OptionalGroup;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OptionalGroupRepository extends JpaRepository<OptionalGroup, Long> {
  public Page<OptionalGroup> findAll(Pageable pageable);
}
