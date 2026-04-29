package com.timetable.timetable.domain.schedule.repository;

import java.util.List;
import java.util.Optional;

import com.timetable.timetable.domain.schedule.entity.Cohort;
import com.timetable.timetable.domain.schedule.entity.CohortSubject;
import com.timetable.timetable.domain.schedule.entity.Subject;
import com.timetable.timetable.domain.user.entity.ApplicationUser;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface CohortSubjectRepository extends JpaRepository<CohortSubject, Long> {

    boolean existsByCohortAndSubjectAndAcademicYearAndSemester(
            Cohort cohort, Subject subject, int academicYear, int semester);

    /**
     * Finds all active cohort-subject combinations for a specific academic period.
     * This is the main query used to prepare data for the solver.
     */
    List<CohortSubject> findByAcademicYearAndSemesterAndIsActive(
            int academicYear,
            int semester,
            boolean isActive);

    @Query("""
                SELECT cs FROM CohortSubject cs
                JOIN FETCH cs.cohort
                JOIN FETCH cs.subject
                JOIN FETCH cs.assignedTeacher
                WHERE cs.id = :id
            """)
    Optional<CohortSubject> findByIdWithDetails(@Param("id") Long id);

    @Override
    @EntityGraph(attributePaths = { "cohort", "subject", "assignedTeacher" })
    Page<CohortSubject> findAll(Pageable pageable);

    @EntityGraph(attributePaths = { "subject", "assignedTeacher" })
    Page<CohortSubject> findByCohort(Cohort cohort, Pageable pageable);

    @EntityGraph(attributePaths = { "cohort", "assignedTeacher" })
    Page<CohortSubject> findBySubject(Subject subject, Pageable pageable);

    @EntityGraph(attributePaths = { "cohort", "subject" })
    Page<CohortSubject> findByAssignedTeacher(ApplicationUser teacher, Pageable pageable);

    @Query("""
                SELECT cs FROM CohortSubject cs
                JOIN FETCH cs.cohort
                JOIN FETCH cs.subject
                JOIN FETCH cs.assignedTeacher
                WHERE cs.academicYear = :academicYear
                  AND cs.semester = :semester
            """)
    List<CohortSubject> findByAcademicYearAndSemester(
            @Param("academicYear") int academicYear,
            @Param("semester") int semester);

    @Query("""
                SELECT cs FROM CohortSubject cs
                JOIN FETCH cs.subject
                JOIN FETCH cs.assignedTeacher
                WHERE cs.cohort = :cohort
                  AND cs.isActive = true
            """)
    List<CohortSubject> findActiveByCohort(@Param("cohort") Cohort cohort);

    /*
     * -----------------------------
     * CÁLCULOS DERIVADOS DE CRÉDITOS
     * -----------------------------
     */

    @Query("""
                SELECT COALESCE(SUM(s.credits), 0)
                FROM CohortSubject cs
                JOIN cs.subject s
                WHERE cs.assignedTeacher = :teacher
                  AND cs.isActive = true
            """)
    int sumCreditsByTeacher(@Param("teacher") ApplicationUser teacher);

    @Query("""
                SELECT COALESCE(SUM(s.credits), 0)
                FROM CohortSubject cs
                JOIN cs.subject s
                WHERE cs.cohort = :cohort
                  AND cs.isActive = true
            """)
    int sumCreditsByCohort(@Param("cohort") Cohort cohort);

    @Modifying
    @Query("DELETE FROM CohortSubject cs WHERE cs.cohort.id = :cohortId")
    void deleteByCohortId(@Param("cohortId") Long cohortId);

    @Modifying
    @Query("DELETE FROM CohortSubject cs WHERE cs.subject.id = :subjectId")
    void deleteBySubjectId(@Param("subjectId") Long subjectId);

    @Modifying
    @Query("DELETE FROM CohortSubject cs WHERE cs.cohort.course.id = :courseId")
    void deleteByCourseId(@Param("courseId") Long courseId);
}
