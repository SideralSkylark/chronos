package com.timetable.timetable.domain.schedule.repository;

import com.timetable.timetable.domain.schedule.entity.Cohort;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface CohortRepository
    extends JpaRepository<Cohort, Long>, JpaSpecificationExecutor<Cohort> {

  boolean existsByYearAndSectionAndSemesterAndAcademicYearAndCourseId(
      int year, String section, int semester, int academicYear, Long courseId);

  long countByCourseIdAndYearAndAcademicYearAndSemester(
      Long courseId, int year, int academicYear, int semester);

  @Query(
      "SELECT CASE WHEN COUNT(c) > 0 THEN true ELSE false END "
          + "FROM Cohort c WHERE c.year = :year AND c.section = :section "
          + "AND c.semester = :semester AND c.academicYear = :academicYear "
          + "AND c.course.id = :courseId AND c.id <> :excludeId")
  boolean existsAnotherWithSameAttributes(
      @Param("year") int year,
      @Param("section") String section,
      @Param("semester") int semester,
      @Param("academicYear") int academicYear,
      @Param("courseId") Long courseId,
      @Param("excludeId") Long excludeId);

  @EntityGraph(attributePaths = {"students", "course"})
  @Query("SELECT c FROM Cohort c WHERE c.id = :id")
  Optional<Cohort> findByIdWithStudentsAndCourse(@Param("id") Long id);

  @EntityGraph(attributePaths = {"course"})
  @Query("SELECT c FROM Cohort c JOIN FETCH c.course WHERE c.id = :id")
  Optional<Cohort> findByIdWithCourse(@Param("id") Long id);

  @EntityGraph(attributePaths = {"course"})
  Page<Cohort> findAll(Specification<Cohort> spec, Pageable pageable);

  List<Cohort> findByAcademicYearAndSemester(int academicYear, int semester);

  List<Cohort> findBySemesterAndAcademicYearAndCourseId(int semester, int year, Long courseId);

  @Modifying
  @Query("DELETE FROM Cohort c WHERE c.course.id = :courseId")
  void deleteByCourseId(@Param("courseId") Long courseId);
}
