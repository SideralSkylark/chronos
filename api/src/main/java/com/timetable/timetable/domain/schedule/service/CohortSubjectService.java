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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CohortSubjectService {

  private final CohortSubjectRepository cohortSubjectRepository;
  private final CohortRepository cohortRepository;
  private final SubjectService subjectService;
  private final UserService userService;

  @Transactional
  public CohortSubject createCohortSubject(CreateCohortSubjectRequest request) {
    log.debug("Creating cohort subject assignment");

    Cohort cohort =
        cohortRepository
            .findByIdWithCourse(request.cohortId())
            .orElseThrow(
                () -> new CohortNotFoundException("Cohort not found: " + request.cohortId()));

    Subject subject = subjectService.findOrThrow(request.subjectId());
    ApplicationUser teacher = userService.findOrThrow(request.assignedTeacherId());

    validateTeacherIsEligible(teacher, subject);
    validateCohortSubjectCompatibility(cohort, subject);
    validateTeacherWorkload(teacher, subject, cohort.getAcademicYear(), cohort.getSemester());

    if (cohortSubjectRepository.existsByCohortAndSubjectAndAcademicYearAndSemester(
        cohort, subject, cohort.getAcademicYear(), cohort.getSemester())) {

      throw new IllegalStateException(
          "This subject is already assigned to this cohort for the same academic period");
    }

    CohortSubject cohortSubject =
        CohortSubject.builder()
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

  public CohortSubject findWithDetailsOrThrow(Long id) {
    return cohortSubjectRepository
        .findByIdWithDetails(id)
        .orElseThrow(
            () ->
                new CohortSubjectNotFoundException(
                    "Cohort subject assignment with id " + id + " not found"));
  }

  @Transactional
  public CohortSubject updateCohortSubject(Long id, UpdateCohortSubjectRequest request) {
    CohortSubject cohortSubject = findWithDetailsOrThrow(id);

    if (!cohortSubject.getAssignedTeacher().getId().equals(request.assignedTeacherId())) {

      ApplicationUser newTeacher = userService.findOrThrow(request.assignedTeacherId());
      boolean isPhantomSwap =
          cohortSubject.getAssignedTeacher().getUsername().startsWith("PHANTOM_");

      validateTeacherIsEligible(newTeacher, cohortSubject.getSubject());

      if (!isPhantomSwap) {
        validateTeacherWorkload(
            newTeacher,
            cohortSubject.getSubject(),
            cohortSubject.getAcademicYear(),
            cohortSubject.getSemester());
      } else {
        log.info(
            "Bypassing workload validation for phantom teacher swap: {} -> {}",
            cohortSubject.getAssignedTeacher().getUsername(),
            newTeacher.getUsername());
      }

      cohortSubject.setAssignedTeacher(newTeacher);
    }

    cohortSubject.setActive(request.isActive());

    return cohortSubjectRepository.save(cohortSubject);
  }

  @Transactional
  public void deleteByCohort(Long id) {
    cohortSubjectRepository.deleteByCohortId(id);
  }

  private void validateTeacherIsEligible(ApplicationUser teacher, Subject subject) {
    if (!teacher.hasRole(UserRole.TEACHER)) {
      throw new IllegalArgumentException("User is not a teacher");
    }

    if (!subject.getEligibleTeachers().contains(teacher)) {
      throw new IllegalArgumentException("Teacher is not eligible to teach this subject");
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

  private void validateTeacherWorkload(
      ApplicationUser teacher, Subject subject, int academicYear, int semester) {
    int currentHours =
        cohortSubjectRepository
            .findByAssignedTeacherAndAcademicYearAndSemesterAndIsActive(
                teacher, academicYear, semester, true)
            .stream()
            .mapToInt(CohortSubject::getWeeklyHours)
            .sum();

    int newHours = AcademicPolicy.calculateWeeklyHours(subject);
    int totalWeeklyHours = currentHours + newHours;

    if (totalWeeklyHours > AcademicPolicy.getWeeklyHoursLimit(teacher)) {
      throw new IllegalArgumentException(
          "Teacher exceeds maximum weekly workload for period " + academicYear + "/" + semester);
    }
  }
}
