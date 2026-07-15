package com.timetable.timetable.domain.schedule.repository;

import com.timetable.timetable.domain.schedule.entity.*;
import com.timetable.timetable.domain.user.entity.ApplicationUser;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ScheduledClassRepository extends JpaRepository<ScheduledClass, Long> {

  @Override
  @EntityGraph(
      attributePaths = {
        "cohortSubject",
        "cohortSubject.cohort",
        "cohortSubject.subject",
        "cohortSubject.assignedTeacher",
        "room",
        "timeslot",
        "timetable"
      })
  Page<ScheduledClass> findAll(Pageable pageable);

  List<ScheduledClass> findByTimetableAndPinnedTrue(Timetable timetable);

  void deleteByTimetableAndPinnedFalse(Timetable timetable);

  boolean existsByCohortSubjectAndTimeslot(CohortSubject cohortSubject, Timeslot timeslot);

  List<ScheduledClass> findByTimetableAndTimeslot(Timetable timetable, Timeslot timeslot);

  @Query(
      """
          SELECT sc FROM ScheduledClass sc
          JOIN FETCH sc.cohortSubject cs
          JOIN FETCH cs.cohort
          JOIN FETCH cs.subject
          JOIN FETCH cs.assignedTeacher
          JOIN FETCH sc.room
          JOIN FETCH sc.timeslot
          LEFT JOIN FETCH sc.timetable
          WHERE sc.id = :id
      """)
  Optional<ScheduledClass> findByIdWithDetails(@Param("id") Long id);

  @Query(
      """
          SELECT sc FROM ScheduledClass sc
          WHERE (
              sc.cohortSubject.assignedTeacher = :teacher
              OR sc.cohortSubject.cohort = :cohort
              OR sc.room = :room
          )
          AND sc.timeslot = :timeslot
          AND (:timetable IS NULL OR sc.timetable = :timetable)
          AND (:excludeId IS NULL OR sc.id <> :excludeId)
      """)
  List<ScheduledClass> findConflicts(
      @Param("teacher") ApplicationUser teacher,
      @Param("cohort") Cohort cohort,
      @Param("room") Room room,
      @Param("timeslot") Timeslot timeslot,
      @Param("timetable") Timetable timetable,
      @Param("excludeId") Long excludeId);

  @Query(
      """
          SELECT sc FROM ScheduledClass sc
          JOIN FETCH sc.cohortSubject cs
          JOIN FETCH cs.cohort co
          JOIN FETCH co.course
          JOIN FETCH cs.subject su
          JOIN FETCH cs.assignedTeacher teacher
          JOIN FETCH sc.timeslot ts
          JOIN FETCH sc.room r
          WHERE sc.timetable.academicYear = :year
            AND sc.timetable.semester    = :semester
      """)
  List<ScheduledClass> findAllWithDetailsByPeriod(
      @Param("year") int year, @Param("semester") int semester);

  @Modifying
  @Query("DELETE FROM ScheduledClass sc WHERE sc.cohortSubject.cohort.id = :cohortId")
  void deleteByCohortId(@Param("cohortId") Long cohortId);

  @Modifying
  @Query("DELETE FROM ScheduledClass sc WHERE sc.cohortSubject.subject.id = :subjectId")
  void deleteBySubjectId(@Param("subjectId") Long subjectId);

  @Modifying
  @Query("DELETE FROM ScheduledClass sc WHERE sc.cohortSubject.cohort.course.id = :courseId")
  void deleteByCourseId(@Param("courseId") Long courseId);
}
