package com.timetable.timetable.domain.schedule.repository;

import com.timetable.timetable.domain.schedule.entity.Course;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CourseRepository extends JpaRepository<Course, Long> {

    boolean existsByName(String name);

    @Query("SELECT c.id FROM Course c")
    Page<Long> findAllIds(Pageable pageable);

    @Query("SELECT c FROM Course c LEFT JOIN FETCH c.expectedCohortsPerAcademicYear WHERE c.id IN :ids")
    List<Course> findAllByIdWithCohorts(List<Long> ids);

    @Query("SELECT c FROM Course c LEFT JOIN FETCH c.expectedCohortsPerAcademicYear WHERE c.id = :id")
    Optional<Course> findByIdWithCohorts(Long id);

    List<Course> findByHasBusinessSimulationTrue();
}
