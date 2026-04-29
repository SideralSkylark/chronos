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

    Page<Subject> findByTargetYearAndTargetSemester(int targetYear, int targetSemester, Pageable pageable);

    List<Subject> findByTargetYearAndTargetSemesterAndCourseId(
            int targetYear, int targetSemester, Long courseId);

    List<Subject> findByCourseIdAndTargetYear(Long courseId, int targetYear);

    List<Subject> findByEligibleTeachersId(Long teacherId);

    List<Subject> findByTargetSemesterAndCourseId(int semester, Long courseId);

    @EntityGraph(attributePaths = { "course", "eligibleTeachers" })
    Optional<Subject> findWithDetailsById(Long id);

    @EntityGraph(attributePaths = { "eligibleTeachers" })
    List<Subject> findByIdIn(List<Long> ids);

    @Query("SELECT s FROM Subject s WHERE " +
            "(:name IS NULL OR LOWER(s.name) LIKE LOWER(CONCAT('%', :name, '%'))) AND " +
            "(:targetYear IS NULL OR s.targetYear = :targetYear) AND " +
            "(:targetSemester IS NULL OR s.targetSemester = :targetSemester) AND " +
            "(:courseId IS NULL OR s.course.id = :courseId)")
    Page<Subject> search(
            @Param("name") String name,
            @Param("targetYear") Integer targetYear,
            @Param("targetSemester") Integer targetSemester,
            @Param("courseId") Long courseId,
            Pageable pageable);

    @Query("SELECT s FROM Subject s WHERE " +
            "(:name IS NULL OR LOWER(s.name) LIKE LOWER(CONCAT('%', :name, '%'))) AND " +
            "(:targetYear IS NULL OR s.targetYear = :targetYear) AND " +
            "(:targetSemester IS NULL OR s.targetSemester = :targetSemester) AND " +
            "(:courseId IS NULL OR s.course.id = :courseId)")
    List<Subject> search(
            @Param("name") String name,
            @Param("targetYear") Integer targetYear,
            @Param("targetSemester") Integer targetSemester,
            @Param("courseId") Long courseId);

    @Query("SELECT COUNT(s) FROM Subject s WHERE s.course.id = :courseId")
    long countByCourseId(@Param("courseId") Long courseId);

    @Query("SELECT s.targetYear, COUNT(s) FROM Subject s WHERE s.course.id = :courseId GROUP BY s.targetYear")
    List<Object[]> countSubjectsByYearForCourse(@Param("courseId") Long courseId);

    boolean existsByNameAndCourseAndTargetYearAndTargetSemester(
            String name, Course course, int targetYear, int targetSemester);

    @Modifying
    @Query("DELETE FROM Subject s WHERE s.course.id = :courseId")
    void deleteByCourseId(@Param("courseId") Long courseId);
}
