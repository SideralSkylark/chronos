package com.timetable.timetable.domain.schedule.service;

import com.timetable.timetable.domain.schedule.dto.CreateCohortSubjectRequest;
import com.timetable.timetable.domain.schedule.dto.UpdateCohortSubjectRequest;
import com.timetable.timetable.domain.schedule.entity.AcademicPolicy;
import com.timetable.timetable.domain.schedule.entity.Cohort;
import com.timetable.timetable.domain.schedule.entity.CohortSubject;
import com.timetable.timetable.domain.schedule.entity.Subject;
import com.timetable.timetable.domain.schedule.exception.CohortNotFoundException;
import com.timetable.timetable.domain.schedule.exception.CohortSubjectNotFoundException;
import com.timetable.timetable.domain.schedule.repository.CohortRepository;
import com.timetable.timetable.domain.schedule.repository.CohortSubjectRepository;
import com.timetable.timetable.domain.user.entity.ApplicationUser;
import com.timetable.timetable.domain.user.entity.UserRole;
import com.timetable.timetable.domain.user.service.UserService;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class CohortSubjectService {

    private final CohortSubjectRepository cohortSubjectRepository;
    private final CohortRepository cohortRepository;
    private final SubjectService subjectService;
    private final UserService userService;

    /*
     * --------------------
     * CREATE
     * --------------------
     */

    @Transactional
    public CohortSubject createCohortSubject(CreateCohortSubjectRequest request) {
        log.debug("Creating cohort subject assignment");

        Cohort cohort = cohortRepository.findById(request.cohortId())
                .orElseThrow(() -> new CohortNotFoundException("Cohort not found: " + request.cohortId()));

        Subject subject = subjectService.findOrThrow(request.subjectId());
        ApplicationUser teacher = userService.findOrThrow(request.assignedTeacherId());

        validateTeacherIsEligible(teacher, subject);
        validateCohortSubjectCompatibility(cohort, subject);
        validateTeacherWorkload(teacher, subject.getCredits());

        if (cohortSubjectRepository.existsByCohortAndSubjectAndAcademicYearAndSemester(
                cohort, subject, cohort.getAcademicYear(), cohort.getSemester())) {

            throw new IllegalStateException(
                    "This subject is already assigned to this cohort for the same academic period");
        }

        CohortSubject cohortSubject = CohortSubject.builder()
                .cohort(cohort)
                .subject(subject)
                .assignedTeacher(teacher)
                .academicYear(cohort.getAcademicYear())
                .semester(cohort.getSemester())
                .isActive(true)
                .build();

        CohortSubject saved = cohortSubjectRepository.save(cohortSubject);

        log.info("Cohort subject {} created", saved.getDisplayName());
        return saved;
    }

    /*
     * --------------------
     * READ
     * --------------------
     */

    @Transactional()
    public CohortSubject getById(Long id) {
        return cohortSubjectRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new CohortSubjectNotFoundException(
                        "Cohort subject assignment with id " + id + " not found"));
    }

    @Transactional()
    public Page<CohortSubject> getAll(Pageable pageable) {
        return cohortSubjectRepository.findAll(pageable);
    }

    @Transactional()
    public Page<CohortSubject> getByCohort(Long cohortId, Pageable pageable) {
        Cohort cohort = cohortRepository.findById(cohortId)
                .orElseThrow(() -> new CohortNotFoundException("Cohort not found: " + cohortId));
        return cohortSubjectRepository.findByCohort(cohort, pageable);
    }

    @Transactional()
    public Page<CohortSubject> getByTeacher(Long teacherId, Pageable pageable) {
        ApplicationUser teacher = userService.findOrThrow(teacherId);

        if (!teacher.hasRole(UserRole.TEACHER)) {
            throw new IllegalArgumentException("User is not a teacher");
        }

        return cohortSubjectRepository.findByAssignedTeacher(teacher, pageable);
    }

    /*
     * --------------------
     * UPDATE
     * --------------------
     */

    @Transactional
    public CohortSubject updateCohortSubject(Long id, UpdateCohortSubjectRequest request) {
        CohortSubject cohortSubject = getById(id);

        if (!cohortSubject.getAssignedTeacher().getId()
                .equals(request.assignedTeacherId())) {

            ApplicationUser newTeacher = userService.findOrThrow(request.assignedTeacherId());

            validateTeacherIsEligible(newTeacher, cohortSubject.getSubject());
            validateTeacherWorkload(newTeacher,
                    cohortSubject.getSubject().getCredits());

            cohortSubject.setAssignedTeacher(newTeacher);
        }

        cohortSubject.setActive(request.isActive());

        return cohortSubjectRepository.save(cohortSubject);
    }

    /*
     * --------------------
     * DELETE
     * --------------------
     */

    @Transactional
    public void deleteCohortSubject(Long id) {
        CohortSubject cohortSubject = getById(id);
        cohortSubjectRepository.delete(cohortSubject);
    }

    @Transactional
    public void deleteByCohort(Long id) {
        cohortSubjectRepository.deleteByCohortId(id);
    }

    /*
     * --------------------
     * AGGREGATES
     * --------------------
     */

    @Transactional()
    public int getTotalWeeklyHoursForCohort(Long cohortId) {
        Cohort cohort = cohortRepository.findById(cohortId)
                .orElseThrow(() -> new CohortNotFoundException("Cohort not found: " + cohortId));
        int credits = cohortSubjectRepository.sumCreditsByCohort(cohort);
        return AcademicPolicy.calculateWeeklyHours(credits);
    }

    /*
     * --------------------
     * VALIDATIONS
     * --------------------
     */

    private void validateTeacherIsEligible(ApplicationUser teacher, Subject subject) {
        if (!teacher.hasRole(UserRole.TEACHER)) {
            throw new IllegalArgumentException("User is not a teacher");
        }

        if (!subject.getEligibleTeachers().contains(teacher)) {
            throw new IllegalArgumentException(
                    "Teacher is not eligible to teach this subject");
        }
    }

    private void validateCohortSubjectCompatibility(Cohort cohort, Subject subject) {
        if (!cohort.getCourse().equals(subject.getCourse())) {
            throw new IllegalArgumentException("Course mismatch");
        }

        if (cohort.getSemester() != subject.getTargetSemester()) {
            throw new IllegalArgumentException("Semester mismatch");
        }
    }

    private void validateTeacherWorkload(ApplicationUser teacher, int newCredits) {
        int currentCredits = cohortSubjectRepository.sumCreditsByTeacher(teacher);

        int totalWeeklyHours = AcademicPolicy.calculateWeeklyHours(currentCredits + newCredits);

        if (totalWeeklyHours > AcademicPolicy.WEEKLY_TEACHING_HOURS_LIMIT) {
            throw new IllegalArgumentException(
                    "Teacher exceeds maximum weekly workload");
        }
    }
}
