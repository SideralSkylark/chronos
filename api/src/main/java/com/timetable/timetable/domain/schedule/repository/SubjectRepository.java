package com.timetable.timetable.domain.schedule.repository;

import java.util.List;
import java.util.Optional;

import com.timetable.timetable.domain.schedule.entity.Course;
import com.timetable.timetable.domain.schedule.entity.Subject;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface SubjectRepository extends JpaRepository<Subject, Long> {

    boolean existsByNameAndTargetYearAndTargetSemesterAndCourse(
            String name, int targetYear, int targetSemester, Course course);

    @Query("SELECT CASE WHEN COUNT(s) > 0 THEN true ELSE false END " +
            "FROM Subject s WHERE s.name = :name AND s.targetYear = :targetYear " +
            "AND s.targetSemester = :targetSemester AND s.course = :course AND s.id <> :excludeId")
    boolean existsAnotherWithSameAttributes(
            @Param("name") String name,
            @Param("targetYear") int targetYear,
            @Param("targetSemester") int targetSemester,
            @Param("course") Course course,
            @Param("excludeId") Long excludeId);

    @EntityGraph(attributePaths = { "eligibleTeachers" })
    Page<Subject> findByCourse(Course course, Pageable pageable);

    List<Subject> findByTargetSemesterAndCourseId(int semester, Long courseId);

    @EntityGraph(attributePaths = { "course", "eligibleTeachers" })
    Optional<Subject> findWithDetailsById(Long id);

    @Query("SELECT s.course.id, COUNT(s) FROM Subject s WHERE s.course.id IN :courseIds GROUP BY s.course.id")
    List<Object[]> countByCourseIds(@Param("courseIds") List<Long> courseIds);

    boolean existsByNameAndCourseAndTargetYearAndTargetSemester(
            String name, Course course, int targetYear, int targetSemester);

    @Modifying
    @Query("DELETE FROM Subject s WHERE s.course.id = :courseId")
    void deleteByCourseId(@Param("courseId") Long courseId);
}
