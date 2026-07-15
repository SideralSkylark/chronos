package com.timetable.timetable.domain.schedule.repository;

import com.timetable.timetable.domain.schedule.entity.Room;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface RoomRepository extends JpaRepository<Room, Long>, JpaSpecificationExecutor<Room> {
  boolean existsByName(String name);

  @EntityGraph(attributePaths = {"restrictions", "restrictions.course"})
  Page<Room> findAll(Pageable pageable);

  @Override
  @EntityGraph(attributePaths = {"restrictions", "restrictions.course"})
  Page<Room> findAll(Specification<Room> spec, Pageable pageable);

  @EntityGraph(attributePaths = {"restrictions", "restrictions.course"})
  java.util.Optional<Room> findById(Long id);

  @Query("SELECT MAX(r.capacity) FROM Room r")
  int findMaxCapacity();
}
