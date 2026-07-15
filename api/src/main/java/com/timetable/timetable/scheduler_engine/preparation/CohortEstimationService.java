package com.timetable.timetable.scheduler_engine.preparation;

import com.timetable.timetable.domain.schedule.dto.CohortEstimationConfig;
import com.timetable.timetable.domain.schedule.dto.CohortEstimationResult;
import com.timetable.timetable.domain.schedule.entity.*;
import com.timetable.timetable.domain.schedule.repository.*;
import com.timetable.timetable.domain.user.entity.ApplicationUser;
import java.util.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class CohortEstimationService {

  private final CohortRepository cohortRepository;

  /**
   * Guarantees the existence of cohorts for the specified period. If none exist, creates them based
   * on the course's expectedCohortsPerAcademicYear map (when defined) or falls back to the provided
   * {@link CohortEstimationConfig}.
   */
  @Transactional
  public CohortEstimationResult ensureCohortsExist(
      Course course, int academicYear, int semester, CohortEstimationConfig config) {

    List<Cohort> existing =
        cohortRepository.findBySemesterAndAcademicYearAndCourseId(
            semester, academicYear, course.getId());

    if (!existing.isEmpty()) {
      log.info(
          "Found {} existing cohorts for {} {}.{}",
          existing.size(),
          course.getName(),
          academicYear,
          semester);
      return new CohortEstimationResult(existing, new ArrayList<>(), false);
    }

    log.info(
        "No cohorts found for {} {}.{}. Generating estimates...",
        course.getName(),
        academicYear,
        semester);

    List<Cohort> generated = generateEstimatedCohorts(course, academicYear, semester, config);

    cohortRepository.saveAll(generated);

    log.info("Generated {} estimated cohorts for {}", generated.size(), course.getName());

    List<String> warnings =
        generated.stream()
            .map(
                c ->
                    "Estimated cohort created: %s (est. %d students)"
                        .formatted(c.getDisplayName(), c.getEstimatedStudentCount()))
            .toList();

    return new CohortEstimationResult(generated, warnings, true);
  }

  /** Confirms a cohort when real enrolment data arrives. */
  @Transactional
  public void confirmCohort(Long cohortId, Set<ApplicationUser> realStudents) {
    Cohort cohort =
        cohortRepository
            .findById(cohortId)
            .orElseThrow(() -> new IllegalArgumentException("Cohort not found: " + cohortId));

    cohort.setStudents(realStudents);
    cohort.setStatus(CohortStatus.CONFIRMED);
    cohortRepository.save(cohort);

    log.info(
        "Cohort {} confirmed with {} real students", cohort.getDisplayName(), realStudents.size());
  }

  // ── Private ────────────────────────────────────────────────────────────────

  private List<Cohort> generateEstimatedCohorts(
      Course course, int academicYear, int semester, CohortEstimationConfig config) {

    Map<Integer, Integer> expectedCohorts = course.getExpectedCohortsPerAcademicYear();

    if (expectedCohorts != null && !expectedCohorts.isEmpty()) {
      log.info("Using expectedCohortsPerAcademicYear map for course {}", course.getName());
      return generateFromExpectedMap(course, academicYear, semester, expectedCohorts);
    }

    log.info(
        "No expectedCohortsPerAcademicYear defined for {}. Falling back to config.",
        course.getName());
    return generateFromConfig(course, academicYear, semester, config);
  }

  /**
   * Generates cohorts from the course's expectedCohortsPerAcademicYear map. Key = curricular year
   * (1..n), Value = number of cohorts for that year.
   */
  private List<Cohort> generateFromExpectedMap(
      Course course, int academicYear, int semester, Map<Integer, Integer> expectedCohorts) {

    List<Cohort> cohorts = new ArrayList<>();

    for (Map.Entry<Integer, Integer> entry : expectedCohorts.entrySet()) {
      int curricularYear = entry.getKey();
      int cohortCount = entry.getValue();

      for (int i = 0; i < cohortCount; i++) {
        String section = String.valueOf((char) ('A' + i));
        cohorts.add(buildCohort(course, academicYear, semester, curricularYear, section));
        log.debug("Generated from map: {}º Ano Turma {}", curricularYear, section);
      }
    }

    return cohorts;
  }

  /**
   * Generates cohorts based on the fallback {@link CohortEstimationConfig}. Iterates all curricular
   * years defined by the course duration.
   */
  private List<Cohort> generateFromConfig(
      Course course, int academicYear, int semester, CohortEstimationConfig config) {

    List<Cohort> cohorts = new ArrayList<>();

    for (int year = 1; year <= course.getYears(); year++) {
      int cohortCount = config.cohortsForYear(year);

      for (int i = 0; i < cohortCount; i++) {
        String section = String.valueOf((char) ('A' + i));
        cohorts.add(buildCohort(course, academicYear, semester, year, section));
        log.debug(
            "Generated from config: {}º Ano Turma {} (est. {} students)",
            year,
            section,
            config.estimatedStudentsPerCohort());
      }
    }

    return cohorts;
  }

  private Cohort buildCohort(
      Course course, int academicYear, int semester, int curricularYear, String section) {

    return Cohort.builder()
        .year(curricularYear)
        .section(section)
        .academicYear(academicYear)
        .semester(semester)
        .course(course)
        .courseNameSnapshot(course.getName())
        .status(CohortStatus.ESTIMATED)
        .estimatedStudentCount(
            AcademicPolicy.ESTIMATED_STUDENT_COUNT) // sensible default; config not available in map
        // path
        .students(new HashSet<>())
        .build();
  }
}
