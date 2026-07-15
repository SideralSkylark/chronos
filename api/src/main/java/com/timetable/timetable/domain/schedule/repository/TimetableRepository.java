package com.timetable.timetable.domain.schedule.repository;

import com.timetable.timetable.domain.schedule.entity.Timetable;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface TimetableRepository extends JpaRepository<Timetable, Long> {
  Optional<Timetable> findFirstByOrderByAcademicYearDescSemesterDesc();

  Optional<Timetable> findByAcademicYearAndSemester(int academicYear, int semester);

  boolean existsByAcademicYearAndSemester(int academicYear, int semester);

  @Query("SELECT COUNT(sc) FROM ScheduledClass sc WHERE sc.timetable.id = :timetableId")
  long countScheduledClasses(@Param("timetableId") Long timetableId);

  @Query(
      """
          SELECT DISTINCT cs.cohort.course.coordinator.id
          FROM CohortSubject cs
          JOIN cs.cohort co
          JOIN co.course c
          WHERE cs.cohort.id IN (
              SELECT DISTINCT sc.cohortSubject.cohort.id
              FROM ScheduledClass sc
              WHERE sc.timetable.id = :timetableId
          )
          AND c.coordinator IS NOT NULL
      """)
  Set<Long> findCoordinatorIdsByTimetableId(@Param("timetableId") Long timetableId);

  @Query(
      """
          SELECT DISTINCT t.academicYear
          FROM Timetable t
          ORDER BY t.academicYear DESC
      """)
  List<Integer> findDistinctAcademicYears();
}
