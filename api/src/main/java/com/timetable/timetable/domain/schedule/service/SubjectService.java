package com.timetable.timetable.domain.schedule.service;

import com.timetable.timetable.domain.schedule.dto.CreateOptionalGroupRequest;
import com.timetable.timetable.domain.schedule.dto.CreateSubjectRequest;
import com.timetable.timetable.domain.schedule.dto.OptionalGroupResponse;
import com.timetable.timetable.domain.schedule.dto.SubjectDetailResponse;
import com.timetable.timetable.domain.schedule.dto.UpdateSubjectRequest;
import com.timetable.timetable.domain.schedule.entity.Course;
import com.timetable.timetable.domain.schedule.entity.OptionalGroup;
import com.timetable.timetable.domain.schedule.entity.Subject;
import com.timetable.timetable.domain.schedule.exception.SubjectNotFoundException;
import com.timetable.timetable.domain.schedule.repository.CohortSubjectRepository;
import com.timetable.timetable.domain.schedule.repository.OptionalGroupRepository;
import com.timetable.timetable.domain.schedule.repository.ScheduledClassRepository;
import com.timetable.timetable.domain.schedule.repository.SubjectRepository;
import com.timetable.timetable.domain.user.entity.ApplicationUser;
import com.timetable.timetable.domain.user.entity.UserRole;
import com.timetable.timetable.domain.user.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SubjectService {
  private final SubjectRepository subjectRepository;
  private final CourseService courseService;
  private final UserRepository userRepository;
  private final CohortSubjectRepository cohortSubjectRepository;
  private final ScheduledClassRepository scheduledClassRepository;
  private final OptionalGroupRepository optionalGroupRepository;

  @Transactional
  public SubjectDetailResponse createSubject(CreateSubjectRequest request) {
    log.debug("Creating subject: {}", request.name());

    Course course = courseService.findCourseOrThrow(request.courseId());

    if (subjectRepository.existsByNameAndTargetYearAndTargetSemesterAndCourse(
        request.name(), request.targetYear(), request.targetSemester(), course)) {
      log.warn(
          "Subject '{}' already exists for year {} semester {} in course {}",
          request.name(),
          request.targetYear(),
          request.targetSemester(),
          course.getName());
      throw new IllegalStateException(
          String.format(
              "Subject '%s' already exists for year %d semester %d in this course",
              request.name(), request.targetYear(), request.targetSemester()));
    }

    validateCredits(request.credits());

    validateTargetYearAndSemester(request.targetYear(), request.targetSemester());

    Subject subject =
        Subject.builder()
            .name(request.name())
            .credits(request.credits())
            .targetYear(request.targetYear())
            .targetSemester(request.targetSemester())
            .course(course)
            .eligibleTeachers(fetchEligibleTeachers(request.eligibleTeacherIds()))
            .build();

    Subject saved = subjectRepository.save(subject);

    log.info(
        "Subject {} created: {} ({} credits, Year {}, Semester {})",
        saved.getId(),
        saved.getName(),
        saved.getCredits(),
        saved.getTargetYear(),
        saved.getTargetSemester());
    return SubjectDetailResponse.from(saved);
  }

  @Transactional
  public OptionalGroupResponse createOptionalGroup(CreateOptionalGroupRequest request) {
    List<Subject> subjects = subjectRepository.findAllById(List.of(request.s1(), request.s2()));

    if (subjects.size() != 2) {
      throw new SubjectNotFoundException(
          "Could not find subjects with ids: {} and {}".formatted(request.s1(), request.s2()));
    }

    boolean alreadyInGroup = subjects.stream().anyMatch(s -> s.getOptionalGroup() != null);
    if (alreadyInGroup) {
      throw new IllegalStateException("One or both subjects already belong to a group");
    }

    String groupName = subjects.get(0).getName() + " / " + subjects.get(1).getName();

    OptionalGroup group = OptionalGroup.builder().name(groupName).build();
    subjects.forEach(group::addSubject);

    System.out.println("on creation: " + group.getName() + "; " + group.getSubjects().toString());

    OptionalGroup savedGroup = optionalGroupRepository.save(group);

    System.out.println(
        "post save: " + savedGroup.getName() + "; " + savedGroup.getSubjects().toString());
    return OptionalGroupResponse.from(savedGroup);
  }

  public Page<SubjectDetailResponse> getAllByCourse(Long courseId, Pageable pageable) {
    log.debug("Fetching all subjects for course {}", courseId);

    Course course = courseService.findCourseOrThrow(courseId);
    Page<SubjectDetailResponse> page =
        subjectRepository.findByCourse(course, pageable).map(SubjectDetailResponse::from);

    log.debug("Found {} subjects for course {}", page.getTotalElements(), courseId);
    return page;
  }

  public Subject findOrThrow(Long id) {
    log.debug("Fetching subject {}", id);
    Subject subject =
        subjectRepository
            .findWithDetailsById(id)
            .orElseThrow(
                () ->
                    new SubjectNotFoundException(
                        String.format("Subject with id %d not found", id)));

    log.info("Found subject {}: {}", id, subject.getName());
    return subject;
  }

  public SubjectDetailResponse getById(Long id) {
    return SubjectDetailResponse.from(findOrThrow(id));
  }

  public Page<OptionalGroupResponse> getOptionalGroups(Pageable pageable) {
    Page<OptionalGroupResponse> pagedResponse =
        optionalGroupRepository.findAll(pageable).map(OptionalGroupResponse::from);
    System.out.println(optionalGroupRepository.findAll().toString());

    return pagedResponse;
  }

  @Transactional
  public SubjectDetailResponse updateSubject(Long id, UpdateSubjectRequest request) {
    log.debug("Updating subject {}", id);
    Subject subject = findOrThrow(id);

    if (!subject.getName().equals(request.name())
        || subject.getTargetYear() != request.targetYear()
        || subject.getTargetSemester() != request.targetSemester()) {

      if (subjectRepository.existsAnotherWithSameAttributes(
          request.name(),
          request.targetYear(),
          request.targetSemester(),
          subject.getCourse(),
          id)) {
        log.warn(
            "Another subject with name '{}' already exists for year {} semester {} in this course",
            request.name(),
            request.targetYear(),
            request.targetSemester());
        throw new IllegalArgumentException(
            String.format(
                "Another subject with name '%s' already exists for year %d semester %d in this"
                    + " course",
                request.name(), request.targetYear(), request.targetSemester()));
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

    log.info(
        "Updated subject {}: {} ({} credits, Year {}, Semester {})",
        updated.getId(),
        updated.getName(),
        updated.getCredits(),
        updated.getTargetYear(),
        updated.getTargetSemester());
    return SubjectDetailResponse.from(updated);
  }

  @Transactional
  public void deleteSubject(Long id) {
    log.debug("Deleting subject {}", id);
    if (!subjectRepository.existsById(id)) {
      throw new SubjectNotFoundException(String.format("Subject with id %d not found", id));
    }

    // 1. Delete scheduled classes associated with this subject (via CohortSubject)
    scheduledClassRepository.deleteBySubjectId(id);

    // 2. Delete cohort-subject associations
    cohortSubjectRepository.deleteBySubjectId(id);

    // 3. Finally delete the subject
    subjectRepository.deleteById(id);

    log.info("Subject {} and all its associations deleted", id);
  }

  @Transactional
  public void deleteGroup(Long groupId) {
    OptionalGroup group =
        optionalGroupRepository
            .findById(groupId)
            .orElseThrow(
                () ->
                    new EntityNotFoundException(
                        "could no find optional group: {}".formatted(groupId)));

    group.getSubjects().forEach(s -> s.setOptionalGroup(null));
    subjectRepository.saveAll(group.getSubjects());

    optionalGroupRepository.delete(group);
  }

  private Set<ApplicationUser> fetchEligibleTeachers(List<Long> ids) {
    if (ids == null || ids.isEmpty()) return new HashSet<>();

    List<ApplicationUser> users = userRepository.findAllById(ids);

    if (ids.size() != users.size()) {
      throw new IllegalArgumentException("one or more teacher ids not found");
    }

    users.forEach(
        user -> {
          if (!user.hasRole(UserRole.TEACHER)) {
            throw new IllegalArgumentException("user %d is not a teacher".formatted(user.getId()));
          }
        });

    return new HashSet<>(users);
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
}
