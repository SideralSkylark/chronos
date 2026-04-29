package com.timetable.timetable.domain.schedule.service;

import java.util.HashMap;
import java.util.Map;

import com.timetable.timetable.config.BusinessSimulationInitializer;
import com.timetable.timetable.domain.schedule.dto.CreateCourseRequest;
import com.timetable.timetable.domain.schedule.dto.UpdateCourseRequest;
import com.timetable.timetable.domain.schedule.entity.Course;
import com.timetable.timetable.domain.schedule.exception.CourseNotFoundException;
import com.timetable.timetable.domain.schedule.repository.CohortRepository;
import com.timetable.timetable.domain.schedule.repository.CohortSubjectRepository;
import com.timetable.timetable.domain.schedule.repository.CourseRepository;
import com.timetable.timetable.domain.schedule.repository.ScheduledClassRepository;
import com.timetable.timetable.domain.schedule.repository.SubjectRepository;
import com.timetable.timetable.domain.user.entity.ApplicationUser;
import com.timetable.timetable.domain.user.entity.UserRole;
import com.timetable.timetable.domain.user.service.UserService;

import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class CourseService {
    private final CourseRepository courseRepository;
    private final UserService userService;
    private final BusinessSimulationInitializer businessSimulationInitializer;
    private final ScheduledClassRepository scheduledClassRepository;
    private final CohortSubjectRepository cohortSubjectRepository;
    private final SubjectRepository subjectRepository;
    private final CohortRepository cohortRepository;

    public Course createCourse(CreateCourseRequest createRequest) {
        log.debug("Creating course");
        if (courseRepository.existsByName(createRequest.name())) {
            log.warn("Course already exists with name: {}", createRequest.name());
            throw new IllegalStateException("Course already exists");
        }

        ApplicationUser coordinator = userService.getUserById(createRequest.coordinatorId());

        if (!coordinator.hasRole(UserRole.COORDINATOR)) {
            log.warn("User {}, is not a coordinator", createRequest.coordinatorId());
            throw new IllegalArgumentException(
                    "User %d is not a coordinator".formatted(createRequest.coordinatorId()));
        }

        int years = createRequest.years() != null ? createRequest.years() : 4;
        Map<Integer, Integer> cohorts = createRequest.expectedCohortsPerYear() != null
                ? createRequest.expectedCohortsPerYear()
                : buildDefaultCohorts(years);

        validateCohortConfig(years, cohorts);

        Course course = Course.builder()
                .name(createRequest.name())
                .coordinator(coordinator)
                .years(years)
                .hasBusinessSimulation(createRequest.hasBusinessSimulation())
                .expectedCohortsPerAcademicYear(cohorts)
                .build();

        Course saved = courseRepository.save(course);

        if (saved.isHasBusinessSimulation()) {
            businessSimulationInitializer.initSimulationForCourse(saved);
        }

        log.info("Course {} created", saved.getId());
        return saved;
    }

    public Course getById(Long id) {
        return courseRepository.findById(id)
                .orElseThrow(() -> new CourseNotFoundException("No course with id: %d".formatted(id)));
    }

    @Transactional
    public Course updateCourse(Long id, UpdateCourseRequest updateRequest) {
        log.debug("Updating course {}", id);
        Course course = getById(id);

        if (!course.getName().equals(updateRequest.name()) && courseRepository.existsByName(updateRequest.name())) {
            log.warn("Another course with that name already exists");
            throw new IllegalArgumentException("Another course with that name already exists.");
        }

        ApplicationUser coordinator = userService.getUserById(updateRequest.coordinatorId());

        if (!coordinator.hasRole(UserRole.COORDINATOR)) {
            log.warn("User {} is not a coordinator", updateRequest.coordinatorId());
            throw new IllegalArgumentException(
                    "User %d is not a coordinator".formatted(updateRequest.coordinatorId()));
        }

        course.setName(updateRequest.name());
        course.setCoordinator(coordinator);

        boolean wasSimulation = course.isHasBusinessSimulation();
        course.setHasBusinessSimulation(updateRequest.hasBusinessSimulation());

        if (updateRequest.years() != null) {
            course.setYears(updateRequest.years());
        }

        if (updateRequest.expectedCohortsPerYear() != null) {
            course.setExpectedCohortsPerAcademicYear(updateRequest.expectedCohortsPerYear());
        }

        Course updated = courseRepository.save(course);

        if (!wasSimulation && updated.isHasBusinessSimulation()) {
            businessSimulationInitializer.initSimulationForCourse(updated);
        }

        log.info("Course {} updated", id);
        return updated;
    }

    @Transactional
    public void deleteCourse(Long id) {
        log.debug("Deleting course {}", id);
        if (!courseRepository.existsById(id)) {
            throw new IllegalArgumentException(
                    "No course with id: %d".formatted(id));
        }

        // 1. Delete scheduled classes associated with this course
        scheduledClassRepository.deleteByCourseId(id);

        // 2. Delete cohort-subject associations
        cohortSubjectRepository.deleteByCourseId(id);

        // 3. Delete subjects
        subjectRepository.deleteByCourseId(id);

        // 4. Delete cohorts
        cohortRepository.deleteByCourseId(id);

        // 5. Finally delete the course
        courseRepository.deleteById(id);
        
        log.info("Course {} and all its associations (subjects, cohorts, etc.) deleted", id);
    }

    private Map<Integer, Integer> buildDefaultCohorts(int years) {
        Map<Integer, Integer> defaults = new HashMap<>();

        for (int year = 1; year <= years; year++) {
            defaults.put(year, 2);
        }

        return defaults;
    }

    private void validateCohortConfig(int years, Map<Integer, Integer> cohorts) {
        cohorts.forEach((year, value) -> {
            if (year > years) {
                throw new IllegalArgumentException(
                        "Cohort configuration contains invalid year: " + year);
            }

            if (value <= 0) {
                throw new IllegalArgumentException(
                        "Cohort number must be positive");
            }
        });
    }
}
