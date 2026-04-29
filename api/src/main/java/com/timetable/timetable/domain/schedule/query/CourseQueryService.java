package com.timetable.timetable.domain.schedule.query;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.timetable.timetable.domain.schedule.dto.CoordinatorOption;
import com.timetable.timetable.domain.schedule.dto.CourseListResponse;
import com.timetable.timetable.domain.schedule.entity.Course;
import com.timetable.timetable.domain.schedule.exception.CourseNotFoundException;
import com.timetable.timetable.domain.schedule.repository.CourseRepository;
import com.timetable.timetable.domain.schedule.repository.SubjectRepository;
import com.timetable.timetable.domain.user.entity.UserRole;
import com.timetable.timetable.domain.user.repository.UserRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class CourseQueryService {
    private final CourseRepository courseRepository;
    private final SubjectRepository subjectRepository;
    private final UserRepository userRepository;

    public Page<Course> getAll(Pageable pageable) {
        return courseRepository.findAll(pageable);
    }

    public Page<CourseListResponse> findAllWithSubjectCount(Pageable pageable) {
        // Step 1: paginated ID query (no JOIN FETCH, so LIMIT works correctly)
        Page<Long> idPage = courseRepository.findAllIds(pageable);

        if (idPage.isEmpty()) {
            return Page.empty(pageable);
        }

        // Step 2: fetch full entities with the map in a single query
        List<Course> courses = courseRepository.findAllByIdWithCohorts(idPage.getContent());

        // Preserve the original page order
        Map<Long, Course> courseById = courses.stream()
                .collect(Collectors.toMap(Course::getId, Function.identity()));

        List<CourseListResponse> responses = idPage.getContent().stream()
                .map(id -> {
                    Course course = courseById.get(id);
                    Long subjectCount = subjectRepository.countByCourseId(id);
                    return CourseListResponse.from(course, subjectCount);
                })
                .collect(Collectors.toList());

        return new PageImpl<>(responses, pageable, idPage.getTotalElements());
    }

    public Course getById(Long id) {
        log.debug("Looking for course {}", id);
        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new CourseNotFoundException("No course with id: %d".formatted(id)));

        log.info("Course {} found", id);
        return course;
    }

    public Page<CoordinatorOption> getAvailableCoordinators(Pageable pageable) {
        return userRepository.findAllByRole(UserRole.COORDINATOR, pageable).map(CoordinatorOption::from);
    }
}
