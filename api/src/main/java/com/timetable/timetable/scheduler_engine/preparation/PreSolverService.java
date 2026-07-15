package com.timetable.timetable.scheduler_engine.preparation;

import com.timetable.timetable.domain.schedule.dto.CohortEstimationConfig;
import com.timetable.timetable.domain.schedule.dto.CohortEstimationResult;
import com.timetable.timetable.domain.schedule.dto.PreSolverRequest;
import com.timetable.timetable.domain.schedule.dto.PreSolverResult;
import com.timetable.timetable.domain.schedule.dto.TeacherAssignmentResult;
import com.timetable.timetable.domain.schedule.entity.*;
import com.timetable.timetable.domain.schedule.repository.*;
import com.timetable.timetable.domain.user.entity.ApplicationUser;
import com.timetable.timetable.domain.user.repository.UserRepository;
import java.util.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Orchestrates data preparation before the solver runs. Ensures all {@link CohortSubject} records
 * exist with teachers assigned.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PreSolverService {

  private final CourseRepository courseRepository;
  private final SubjectRepository subjectRepository;
  private final CohortSubjectRepository cohortSubjectRepository;
  private final TimetableRepository timetableRepository;
  private final FixedDaySessionService fixedDaySessionService;
  private final CohortEstimationService cohortEstimation;
  private final TeacherAssignmentService teacherAssignment;
  private final UserRepository userRepository;

  @Transactional
  public PreSolverResult prepare(PreSolverRequest request) {
    log.info("=".repeat(60));
    log.info("PRE-SOLVER PREPARATION for {}.{}", request.academicYear(), request.semester());
    log.info("=".repeat(60));

    List<Course> courses = resolveCourses(request);
    log.info("Processing {} courses", courses.size());

    // Load once — updated in-memory as new CohortSubjects are created
    List<CohortSubject> activeCohortSubjects =
        cohortSubjectRepository.findByAcademicYearAndSemesterAndIsActive(
            request.academicYear(), request.semester(), true);

    PreSolverStats stats = new PreSolverStats();

    for (Course course : courses) {
      processCourse(course, request, activeCohortSubjects, stats);
    }

    Timetable timetable =
        timetableRepository
            .findByAcademicYearAndSemester(request.academicYear(), request.semester())
            .orElseGet(
                () -> {
                  log.info(
                      "Creating timetable for {}.{}", request.academicYear(), request.semester());
                  return timetableRepository.save(
                      Timetable.builder()
                          .academicYear(request.academicYear())
                          .semester(request.semester())
                          .status(TimetableStatus.DRAFT)
                          .build());
                });

    List<CohortSubject> allActive =
        cohortSubjectRepository.findByAcademicYearAndSemesterAndIsActive(
            request.academicYear(), request.semester(), true);

    int preAssigned = fixedDaySessionService.preAssign(allActive, timetable);
    stats.preAssignedClasses = preAssigned;

    stats.log();
    return stats.toResult();
  }

  private List<Course> resolveCourses(PreSolverRequest request) {
    return request.courseIds() != null
        ? courseRepository.findAllById(request.courseIds())
        : courseRepository.findAll();
  }

  private void processCourse(
      Course course,
      PreSolverRequest request,
      List<CohortSubject> activeCohortSubjects,
      PreSolverStats stats) {

    CohortEstimationResult cohortResult =
        cohortEstimation.ensureCohortsExist(
            course, request.academicYear(), request.semester(), resolveEstimationConfig(request));

    if (cohortResult.wasGenerated()) {
      stats.estimatedCohorts += cohortResult.cohorts().size();
      stats.warnings.addAll(cohortResult.warnings());
    }

    List<Subject> semesterSubjects =
        subjectRepository.findByTargetSemesterAndCourseId(request.semester(), course.getId());

    for (Cohort cohort : cohortResult.cohorts()) {
      processCohort(cohort, semesterSubjects, request, activeCohortSubjects, stats);
    }
  }

  private void processCohort(
      Cohort cohort,
      List<Subject> semesterSubjects,
      PreSolverRequest request,
      List<CohortSubject> activeCohortSubjects,
      PreSolverStats stats) {

    List<Subject> yearSubjects =
        semesterSubjects.stream().filter(s -> s.getTargetYear() == cohort.getYear()).toList();

    for (Subject subject : yearSubjects) {
      boolean exists =
          cohortSubjectRepository.existsByCohortAndSubjectAndAcademicYearAndSemester(
              cohort, subject, request.academicYear(), request.semester());

      if (exists) {
        log.debug(
            "CohortSubject already exists: {} - {}", cohort.getDisplayName(), subject.getName());
        continue;
      }

      createCohortSubject(cohort, subject, request, activeCohortSubjects, stats);
    }
  }

  private void createCohortSubject(
      Cohort cohort,
      Subject subject,
      PreSolverRequest request,
      List<CohortSubject> activeCohortSubjects,
      PreSolverStats stats) {

    TeacherAssignmentResult assignment;

    if (subject.isFixedDaySession()) {
      // Simulação Empresarial — força "A Equipa"
      ApplicationUser simulationTeam =
          userRepository
              .findByUsername("A Equipa")
              .orElseThrow(
                  () ->
                      new IllegalStateException(
                          "Simulation team user not found — run BusinessSimulationInitializer"
                              + " first"));
      assignment = new TeacherAssignmentResult(simulationTeam, false, null);
    } else {
      assignment = teacherAssignment.assignTeacher(subject, activeCohortSubjects, request.policy());
    }

    if (assignment.warning() != null) stats.warnings.add(assignment.warning());
    if (assignment.isPhantom()) stats.phantomTeachers++;

    CohortSubject cs =
        CohortSubject.builder()
            .cohort(cohort)
            .subject(subject)
            .assignedTeacher(assignment.teacher())
            .academicYear(request.academicYear())
            .semester(request.semester())
            .isActive(true)
            .build();

    cohortSubjectRepository.save(cs);
    activeCohortSubjects.add(cs); // Keep in-memory workload up to date
    stats.cohortSubjectsCreated++;

    log.debug(
        "Created CohortSubject: {} → {} ({})",
        cohort.getDisplayName(),
        subject.getName(),
        assignment.teacher().getUsername());
  }

  private CohortEstimationConfig resolveEstimationConfig(PreSolverRequest request) {
    return request.estimationConfig() != null
        ? request.estimationConfig()
        : CohortEstimationConfig.defaultConfig();
  }

  private static class PreSolverStats {
    int estimatedCohorts = 0;
    int cohortSubjectsCreated = 0;
    int phantomTeachers = 0;
    int preAssignedClasses = 0;
    List<String> warnings = new ArrayList<>();

    void log() {
      String sep = "=".repeat(60);
      org.slf4j.LoggerFactory.getLogger(PreSolverService.class).info(sep);
      org.slf4j.LoggerFactory.getLogger(PreSolverService.class).info("PREPARATION DONE!");
      org.slf4j.LoggerFactory.getLogger(PreSolverService.class)
          .info("Estimated cohorts:    {}", estimatedCohorts);
      org.slf4j.LoggerFactory.getLogger(PreSolverService.class)
          .info("CohortSubjects:       {}", cohortSubjectsCreated);
      org.slf4j.LoggerFactory.getLogger(PreSolverService.class)
          .info("Phantom teachers:     {}", phantomTeachers);
      org.slf4j.LoggerFactory.getLogger(PreSolverService.class)
          .info("Pre-assigned classes: {}", preAssignedClasses);
      org.slf4j.LoggerFactory.getLogger(PreSolverService.class)
          .info("Warnings:             {}", warnings.size());
      org.slf4j.LoggerFactory.getLogger(PreSolverService.class).info(sep);
    }

    PreSolverResult toResult() {
      return new PreSolverResult(
          estimatedCohorts, cohortSubjectsCreated, phantomTeachers, warnings);
    }
  }
}
