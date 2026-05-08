package com.timetable.timetable.domain.schedule.service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.timetable.timetable.domain.schedule.dto.CohortFilterParams;
import com.timetable.timetable.domain.schedule.dto.CohortListResponse;
import com.timetable.timetable.domain.schedule.dto.CohortResponse;
import com.timetable.timetable.domain.schedule.dto.CohortSummaryResponse;
import com.timetable.timetable.domain.schedule.dto.CreateCohortRequest;
import com.timetable.timetable.domain.schedule.dto.UpdateCohortRequest;
import com.timetable.timetable.domain.schedule.entity.Cohort;
import com.timetable.timetable.domain.schedule.entity.CohortStatus;
import com.timetable.timetable.domain.schedule.entity.Course;
import com.timetable.timetable.domain.schedule.exception.CohortNotFoundException;
import com.timetable.timetable.domain.schedule.repository.CohortRepository;
import com.timetable.timetable.domain.schedule.repository.RoomRepository;
import com.timetable.timetable.domain.schedule.repository.ScheduledClassRepository;
import com.timetable.timetable.domain.schedule.specification.CohortSpecifications;
import com.timetable.timetable.domain.user.entity.ApplicationUser;
import com.timetable.timetable.domain.user.entity.UserRole;
import com.timetable.timetable.domain.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CohortService {
    private final CohortRepository cohortRepository;
    private final CohortSubjectService cohortSubjectService;
    private final CourseService courseService;
    private final UserRepository userRepository;
    private final RoomRepository roomRepository;
    private final ScheduledClassRepository scheduledClassRepository;

    @Transactional
    public Cohort createCohort(CreateCohortRequest createRequest) {
        log.debug("Creating cohort");

        if (cohortRepository.existsByYearAndSectionAndSemesterAndAcademicYearAndCourseId(
                createRequest.year(),
                createRequest.section(),
                createRequest.semester(),
                createRequest.academicYear(),
                createRequest.courseId())) {

            String cohortIdentifier = String.format("%d-%s-%d-%d",
                    createRequest.year(),
                    createRequest.section(),
                    createRequest.semester(),
                    createRequest.academicYear());

            log.warn("Another cohort already exists with specification: {}", cohortIdentifier);

            throw new IllegalStateException(
                    String.format("Cohort '%s' already exists for the designated course", cohortIdentifier));
        }

        Course course = courseService.findCourseOrThrow(createRequest.courseId());

        Integer expectedCohorts = course.getExpectedCohortsPerAcademicYear()
                .get(createRequest.year());

        if (expectedCohorts == null) {
            throw new IllegalStateException(
                    "No cohort limit configured for academic year " + createRequest.year());
        }

        long existingCount = cohortRepository
                .countByCourseIdAndYearAndAcademicYearAndSemester(
                        course.getId(),
                        createRequest.year(),
                        createRequest.academicYear(),
                        createRequest.semester());

        if (existingCount >= expectedCohorts) {
            throw new IllegalStateException(
                    String.format(
                            "Maximum number of cohorts (%d) reached for course %d, academic year %d, semester %d",
                            expectedCohorts,
                            course.getId(),
                            createRequest.academicYear(),
                            createRequest.semester()));
        }

        Set<ApplicationUser> students = new HashSet<>();
        if (createRequest.studentIds() != null && !createRequest.studentIds().isEmpty()) {
            students = validateAndFetchStudents(createRequest.studentIds());
        }

        Cohort cohort = Cohort.builder()
                .year(createRequest.year())
                .section(createRequest.section())
                .semester(createRequest.semester())
                .academicYear(createRequest.academicYear())
                .course(course)
                .courseNameSnapshot(course.getName())
                .students(students)
                .build();

        Cohort saved = cohortRepository.save(cohort);

        log.info("Cohort {} created with identifier: {}", saved.getId(), saved.getDisplayName());

        return saved;
    }

    @Transactional
    public CohortResponse createCohortResponse(CreateCohortRequest request) {
        return CohortResponse.from(createCohort(request));
    }

    @Transactional
    public CohortResponse confirmCohort(Long id, int studentCount) {
        log.debug("starting confirmation");
        Cohort cohort = getById(id);

        if (cohort.getStatus() == CohortStatus.CONFIRMED) {
            throw new IllegalStateException("Turma já confirmada");
        }

        // Valida contra capacidade máxima das salas
        int maxCapacity = roomRepository.findMaxCapacity();
        if (studentCount > maxCapacity) {
            throw new IllegalArgumentException(
                    "Número de alunos (%d) excede a capacidade máxima das salas (%d). Considere dividir em duas turmas."
                            .formatted(studentCount, maxCapacity));
        }

        cohort.setEstimatedStudentCount(studentCount);
        cohort.setStatus(CohortStatus.CONFIRMED);

        Cohort saved = cohortRepository.save(cohort);
        log.info("Cohort {} confirmed with {} students", id, studentCount);
        return CohortResponse.from(saved);
    }

    public Page<CohortListResponse> findAll(Pageable pageable, CohortFilterParams filters) {

        return cohortRepository.findAll(CohortSpecifications.withFilters(filters), pageable)
                .map(cohort -> new CohortListResponse(
                        cohort.getId(),
                        cohort.getYear(),
                        cohort.getSection(),
                        cohort.getAcademicYear(),
                        cohort.getSemester(),
                        cohort.getCourse().getId(),
                        cohort.getCourseNameSnapshot(),
                        cohort.getStudentCount(),
                        cohort.getStatus()));
    }

    public CohortSummaryResponse getSummary(CohortFilterParams filters) {
        long total = cohortRepository.count(CohortSpecifications.withFilters(filters));

        filters.setStatus(CohortStatus.CONFIRMED);
        long confirmed = cohortRepository.count(CohortSpecifications.withFilters(filters));

        return new CohortSummaryResponse(total, confirmed);
    }

    public Cohort getById(Long id) {
        log.debug("Looking for cohort {}", id);
        Cohort cohort = cohortRepository.findByIdWithStudentsAndCourse(id)
            .orElseThrow(() -> new CohortNotFoundException("could not find cohort %d".formatted(id)));
        log.info("Cohort {} found: {}", id, cohort.getDisplayName());
        return cohort;
    }

    public CohortResponse getResponseById(Long id) {
        return CohortResponse.from(getById(id));
    }

    @Transactional
    public CohortResponse updateCohort(Long id, UpdateCohortRequest updateRequest) {
        log.debug("Updating cohort {}", id);
        Cohort cohort = cohortRepository.findByIdWithCourse(id)
                .orElseThrow(() -> new CohortNotFoundException("course d% not found".formatted(id)));

        if (cohortRepository.existsAnotherWithSameAttributes(
                updateRequest.year(),
                updateRequest.section(),
                updateRequest.semester(),
                updateRequest.academicYear(),
                cohort.getCourse().getId(),
                cohort.getId())) {
            log.warn("Another cohort with the same data already exists");
            throw new IllegalArgumentException(
                    "Another cohort with the same specification already exists");
        }

        Set<ApplicationUser> students = validateAndFetchStudents(updateRequest.studentIds());

        cohort.setYear(updateRequest.year());
        cohort.setSection(updateRequest.section());
        cohort.setSemester(updateRequest.semester());
        cohort.setAcademicYear(updateRequest.academicYear());
        cohort.setStudents(students);

        Cohort updated = cohortRepository.save(cohort);

        log.info("Cohort {} updated: {}", updated.getId(), updated.getDisplayName());
        return CohortResponse.from(updated);
    }

    @Transactional
    public CohortResponse updateStudents(Long cohortId, List<Long> studentIds) {
        Cohort cohort = getById(cohortId);

        Set<ApplicationUser> students = validateAndFetchStudents(studentIds);
        cohort.setStudents(students);
        return CohortResponse.from(cohortRepository.save(cohort));
    }

    @Transactional
    public void deleteCohort(Long id) {
        if (!cohortRepository.existsById(id)) {
            throw new CohortNotFoundException(String.format("Cohort with id %d not found", id));
        }

        scheduledClassRepository.deleteByCohortId(id); // 1. scheduled_classes
        cohortSubjectService.deleteByCohort(id); // 2. cohort_subjects
        cohortRepository.deleteById(id); // 3. cohort (cohort_students vai junto)

        log.info("Cohort {} deleted", id);
    }

    private Set<ApplicationUser> validateAndFetchStudents(List<Long> studentIds) {
        if (studentIds == null || studentIds.isEmpty()) return new HashSet<>();

        List<ApplicationUser> users = userRepository.findAllById(studentIds);

        if (users == null || users.isEmpty()) {
            throw new IllegalArgumentException("One or more student ids not found");
        }

        users.forEach(user -> {
            if (!user.hasRole(UserRole.STUDENT)) {
                throw new IllegalArgumentException("user %d is not a student".formatted(user.getId()));
            }
        });

        return new HashSet<>(users);
    }
}
