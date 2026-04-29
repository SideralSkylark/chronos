package com.timetable.timetable.domain.schedule.service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.timetable.timetable.domain.schedule.dto.CreateSubjectRequest;
import com.timetable.timetable.domain.schedule.dto.UpdateSubjectRequest;
import com.timetable.timetable.domain.schedule.entity.Course;
import com.timetable.timetable.domain.schedule.entity.Subject;
import com.timetable.timetable.domain.schedule.exception.SubjectNotFoundException;
import com.timetable.timetable.domain.schedule.repository.CohortSubjectRepository;
import com.timetable.timetable.domain.schedule.repository.ScheduledClassRepository;
import com.timetable.timetable.domain.schedule.repository.SubjectRepository;
import com.timetable.timetable.domain.user.entity.ApplicationUser;
import com.timetable.timetable.domain.user.entity.UserRole;
import com.timetable.timetable.domain.user.service.UserService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class SubjectService {
    private final SubjectRepository subjectRepository;
    private final CourseService courseService;
    private final UserService userService;
    private final CohortSubjectRepository cohortSubjectRepository;
    private final ScheduledClassRepository scheduledClassRepository;

    @Transactional
    public Subject createSubject(CreateSubjectRequest request) {
        log.debug("Creating subject: {}", request.name());
        
        Course course = courseService.getById(request.courseId());

        if (subjectRepository.existsByNameAndTargetYearAndTargetSemesterAndCourse(
            request.name(), 
            request.targetYear(),
            request.targetSemester(),
            course)) {
            log.warn("Subject '{}' already exists for year {} semester {} in course {}", 
                request.name(), request.targetYear(), request.targetSemester(), course.getName());
            throw new IllegalStateException(
                String.format("Subject '%s' already exists for year %d semester %d in this course", 
                    request.name(), request.targetYear(), request.targetSemester())
            );
        }

        validateCredits(request.credits());
        
        validateTargetYearAndSemester(request.targetYear(), request.targetSemester());

        Subject subject = Subject.builder()
            .name(request.name())
            .credits(request.credits())
            .targetYear(request.targetYear())
            .targetSemester(request.targetSemester())
            .course(course)
            .eligibleTeachers(fetchEligibleTeachers(request.eligibleTeacherIds()))
            .build();

        Subject saved = subjectRepository.save(subject);

        log.info("Subject {} created: {} ({} credits, Year {}, Semester {})", 
            saved.getId(), saved.getName(), saved.getCredits(), 
            saved.getTargetYear(), saved.getTargetSemester());
        return saved;
    }

    @Transactional(readOnly = true)
    public Page<Subject> getAllByCourse(Long courseId, Pageable pageable) {
        log.debug("Fetching all subjects for course {}", courseId);

        Course course = courseService.getById(courseId);
        Page<Subject> page = subjectRepository.findByCourse(course, pageable);

        log.debug("Found {} subjects for course {}", page.getTotalElements(), courseId);
        return page;
    }

    @Transactional(readOnly = true)
    public Page<Subject> getByTargetYearAndSemester(int targetYear, int targetSemester, Pageable pageable) {
        log.debug("Fetching subjects for year {} semester {}", targetYear, targetSemester);
        return subjectRepository.findByTargetYearAndTargetSemester(targetYear, targetSemester, pageable);
    }

    @Transactional(readOnly = true)
    public List<Subject> getSubjectsForCohort(int year, int semester, Long courseId) {
        log.debug("Fetching subjects for cohort (year {}, semester {}, course {})", 
            year, semester, courseId);
        return subjectRepository.findByTargetYearAndTargetSemesterAndCourseId(year, semester, courseId);
    }

    public Subject getById(Long id) {
        log.debug("Fetching subject {}", id);
        Subject subject = subjectRepository.findWithDetailsById(id)
            .orElseThrow(() -> new SubjectNotFoundException(
                String.format("Subject with id %d not found", id)
            ));

        log.info("Found subject {}: {}", id, subject.getName());
        return subject;
    }

    @Transactional
    public Subject updateSubject(Long id, UpdateSubjectRequest request) {
        log.debug("Updating subject {}", id);
        Subject subject = getById(id);

        if (!subject.getName().equals(request.name()) || 
            subject.getTargetYear() != request.targetYear() ||
            subject.getTargetSemester() != request.targetSemester()) {
            
            if (subjectRepository.existsAnotherWithSameAttributes(
                request.name(), 
                request.targetYear(),
                request.targetSemester(),
                subject.getCourse(), 
                id)) {
                log.warn("Another subject with name '{}' already exists for year {} semester {} in this course", 
                    request.name(), request.targetYear(), request.targetSemester());
                throw new IllegalArgumentException(
                    String.format("Another subject with name '%s' already exists for year %d semester %d in this course", 
                        request.name(), request.targetYear(), request.targetSemester())
                );
            }
        }

        // Validações
        validateCredits(request.credits());
        validateTargetYearAndSemester(request.targetYear(), request.targetSemester());

        subject.setName(request.name());
        subject.setCredits(request.credits());
        subject.setTargetYear(request.targetYear());
        subject.setTargetSemester(request.targetSemester());
        subject.setEligibleTeachers(fetchEligibleTeachers(request.eligibleTeacherIds()));

        Subject updated = subjectRepository.save(subject);

        log.info("Updated subject {}: {} ({} credits, Year {}, Semester {})", 
            updated.getId(), updated.getName(), updated.getCredits(), 
            updated.getTargetYear(), updated.getTargetSemester());
        return updated;
    }

    @Transactional
    public Long countLessonsByCourseId(Long courseId) {
        return subjectRepository.countByCourseId(courseId);
    }

    @Transactional
    public void deleteSubject(Long id) {
        log.debug("Deleting subject {}", id);
        if (!subjectRepository.existsById(id)) {
            throw new SubjectNotFoundException(
                String.format("Subject with id %d not found", id)
            );
        }

        // 1. Delete scheduled classes associated with this subject (via CohortSubject)
        scheduledClassRepository.deleteBySubjectId(id);
        
        // 2. Delete cohort-subject associations
        cohortSubjectRepository.deleteBySubjectId(id);

        // 3. Finally delete the subject
        subjectRepository.deleteById(id);
        
        log.info("Subject {} and all its associations deleted", id);
    }

    private Set<ApplicationUser> fetchEligibleTeachers(List<Long> ids) {
        Set<ApplicationUser> teachers = new HashSet<>();

        if (ids == null || ids.isEmpty()) {
            return teachers;
        }
        
        for (Long id : ids) {
            ApplicationUser user = userService.getUserById(id);
            
            if (!user.hasRole(UserRole.TEACHER)) {
                throw new IllegalArgumentException(
                    String.format("User with id %d is not a teacher", id)
                );
            }
            
            teachers.add(user);
        }
        
        return teachers;
    }

    private void validateCredits(int credits) {
        if (credits <= 0) {
            throw new IllegalArgumentException("Credits must be greater than 0");
        }
        if (credits > 30) { 
            throw new IllegalArgumentException("Credits cannot exceed 30");
        }
    }

    private void validateTargetYearAndSemester(int targetYear, int targetSemester) {
        if (targetYear <= 0) {
            throw new IllegalArgumentException("Target year must be greater than 0");
        }
        if (targetSemester < 1 || targetSemester > 2) {
            throw new IllegalArgumentException("Target semester must be 1 or 2");
        }
    }

    // Métodos adicionais úteis
    
    @Transactional(readOnly = true)
    public List<Subject> getSubjectsByTeacher(Long teacherId) {
        log.debug("Fetching subjects for teacher {}", teacherId);
        return subjectRepository.findByEligibleTeachersId(teacherId);
    }
    
    @Transactional(readOnly = true)
    public List<Subject> getSubjectsByCourseAndYear(Long courseId, int targetYear) {
        log.debug("Fetching subjects for course {} and year {}", courseId, targetYear);
        return subjectRepository.findByCourseIdAndTargetYear(courseId, targetYear);
    }
    
    @Transactional(readOnly = true)
    public Page<Subject> searchSubjects(String name, Integer targetYear, Integer targetSemester, 
                                        Long courseId, Pageable pageable) {
        log.debug("Searching subjects with filters: name={}, year={}, semester={}, courseId={}", 
            name, targetYear, targetSemester, courseId);
        return subjectRepository.search(name, targetYear, targetSemester, courseId, pageable);
    }
}
