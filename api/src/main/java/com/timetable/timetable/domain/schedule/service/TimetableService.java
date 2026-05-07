package com.timetable.timetable.domain.schedule.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.timetable.timetable.domain.schedule.dto.CandidateTeacherResponse;
import com.timetable.timetable.domain.schedule.dto.CreateTimetableRequest;
import com.timetable.timetable.domain.schedule.dto.ReasignTeacherRequest;
import com.timetable.timetable.domain.schedule.dto.TimetableResponse;
import com.timetable.timetable.domain.schedule.dto.UpdateCohortSubjectRequest;
import com.timetable.timetable.domain.schedule.dto.UpdateTimetableRequest;
import com.timetable.timetable.domain.schedule.entity.AcademicPolicy;
import com.timetable.timetable.domain.schedule.entity.CohortSubject;
import com.timetable.timetable.domain.schedule.entity.ScheduledClass;
import com.timetable.timetable.domain.schedule.entity.Timetable;
import com.timetable.timetable.domain.schedule.entity.TimetableStatus;
import com.timetable.timetable.domain.schedule.exception.ScheduledClassNotFoundException;
import com.timetable.timetable.domain.schedule.exception.TimetableNotFoundException;
import com.timetable.timetable.domain.schedule.repository.TimetableRepository;
import com.timetable.timetable.domain.schedule.repository.CohortSubjectRepository;
import com.timetable.timetable.domain.schedule.repository.ScheduledClassRepository;
import com.timetable.timetable.domain.user.entity.ApplicationUser;
import com.timetable.timetable.domain.user.entity.UserRole;
import com.timetable.timetable.domain.user.exception.UserNotFoundException;
import com.timetable.timetable.domain.user.repository.UserRepository;
import com.timetable.timetable.domain.user.service.NotificationService;
import com.timetable.timetable.security.SecurityUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TimetableService {
    private final TimetableRepository timetableRepository;
    private final ScheduledClassRepository scheduledClassRepository;
    private final UserRepository userRepository;
    private final CohortSubjectRepository cohortSubjectRepository;
    private final CohortSubjectService cohortSubjectService;
    private final NotificationService notificationService;

    @Transactional
    public TimetableResponse createTimetable(CreateTimetableRequest createRequest) {
        log.debug("Creating timetable");
        if (timetableRepository.existsByAcademicYearAndSemester(createRequest.academicYear(),
                createRequest.semester())) {
            log.warn("Timetable for period {} already exists", createRequest.academicYear());
            throw new IllegalStateException(
                    "Timetable for academic period '%s' already exists".formatted(createRequest.academicYear()));
        }

        Timetable timetable = Timetable.builder()
                .academicYear(createRequest.academicYear())
                .semester(createRequest.semester())
                .status(TimetableStatus.DRAFT)
                .build();

        Timetable saved = timetableRepository.save(timetable);

        log.info("Timetable {} created", saved.getId());
        return TimetableResponse.from(saved);
    }

    public Page<TimetableResponse> getAll(Pageable pageable) {
        log.debug("Fetching all timetables");
        return timetableRepository.findAll(pageable).map(TimetableResponse::from);
    }

    public Timetable findOrThrow(Long id) {
        log.debug("fetching timetable {}", id);
        Timetable timetable = timetableRepository.findById(id)
                .orElseThrow(() -> new TimetableNotFoundException(
                        "Timetable with id %d not found".formatted(id)));

        log.info("timetable {} found", timetable.getId());
        return timetable;
    }

    public TimetableResponse getById(Long id) {
        return TimetableResponse.from(findOrThrow(id));
    }

    /**
     * return a list of {@link CandidateTeacherResponse}. To be used for the phantom
     * swap feature.
     */
    @Transactional
    public List<CandidateTeacherResponse> getReplacementCandidates(Long lessonId) {
        ScheduledClass scheduledClass = scheduledClassRepository.findByIdWithDetails(lessonId)
                .orElseThrow(() -> new ScheduledClassNotFoundException(
                        "scheduled class with id %d not found".formatted(lessonId)));

        int academicYear = scheduledClass.getCohortSubject().getAcademicYear();
        int semester = scheduledClass.getCohortSubject().getSemester();

        List<ApplicationUser> allTeachers = userRepository.findAllByRole(UserRole.TEACHER);

        // one query for all cohort subjects in the period — avoids N+1 per teacher
        List<CohortSubject> allCohortSubjects = cohortSubjectRepository
                .findByAcademicYearAndSemester(academicYear, semester);

        Map<Long, Long> countByTeacherId = allCohortSubjects.stream()
                .collect(Collectors.groupingBy(
                        cs -> cs.getAssignedTeacher().getId(),
                        Collectors.counting()));

        List<CandidateTeacherResponse> response = new ArrayList<>();

        for (ApplicationUser teacher : allTeachers) {
            long count = countByTeacherId.getOrDefault(teacher.getId(), 0L);
            int weeklyHours = (int) count * AcademicPolicy.WEEKLY_CONTACT_HOURS;
            int limit = AcademicPolicy.getWeeklyHoursLimit(teacher);
            boolean wouldExceedLimit = weeklyHours + AcademicPolicy.WEEKLY_CONTACT_HOURS > limit;
            boolean qualified = scheduledClass.getSubject().getEligibleTeachers().contains(teacher);

            response.add(CandidateTeacherResponse.from(
                    teacher, weeklyHours, limit, wouldExceedLimit, qualified));
        }

        return response;
    }

    @Transactional
    public TimetableResponse reasignTeacher(Long lessonId, ReasignTeacherRequest request) {
        ScheduledClass scheduledClass = scheduledClassRepository.findByIdWithDetails(lessonId)
                .orElseThrow(() -> new ScheduledClassNotFoundException(
                        "scheduled class not found with id %d".formatted(lessonId)));

        ApplicationUser teacher = userRepository.findById(request.teacherId())
                .orElseThrow(() -> new UserNotFoundException(
                        "could not find teacher %d".formatted(request.teacherId())));

        if (teacher.getUsername().contains("PHANTOM")) {
            throw new IllegalArgumentException("Cannot assign phantom teacher");
        }

        cohortSubjectService.updateCohortSubject(
                scheduledClass.getCohortSubject().getId(),
                UpdateCohortSubjectRequest.from(teacher.getId(), true));

        Timetable timetable = timetableRepository.findByAcademicYearAndSemester(
                scheduledClass.getCohortSubject().getAcademicYear(),
                scheduledClass.getCohortSubject().getSemester())
                .orElseThrow(() -> new TimetableNotFoundException(
                        "Could not find a timetable for %d / %d".formatted(
                                scheduledClass.getCohortSubject().getAcademicYear(),
                                scheduledClass.getCohortSubject().getSemester())));

        return TimetableResponse.from(timetable);
    }

    @Transactional
    public TimetableResponse updateTimetable(Long id, UpdateTimetableRequest updateRequest) {
        log.debug("Updating timetable {}", id);
        Timetable timetable = findOrThrow(id);

        if (!timetable.getAcademicPeriod().equals(
                updateRequest.academicYear() + "." + updateRequest.semester()) &&
                timetableRepository.existsByAcademicYearAndSemester(
                        updateRequest.academicYear(), updateRequest.semester())) {
            log.warn("Another timetable for {} period already exists", updateRequest.academicYear());
            throw new IllegalArgumentException(
                    "Another timetable for academic period '%s' already exists"
                            .formatted(updateRequest.academicYear()));
        }

        timetable.setAcademicYear(updateRequest.academicYear());
        timetable.setSemester(updateRequest.semester());
        timetable.setStatus(updateRequest.status());

        Timetable updated = timetableRepository.save(timetable);
        log.info("Timetable {} updated", updated.getId());
        return TimetableResponse.from(updated);
    }

    @Transactional
    public TimetableResponse submitForApproval(Long id) {
        log.debug("Submitting timetable {} for approval", id);
        Timetable timetable = findOrThrow(id);
        timetable.setStatus(TimetableStatus.PENDING_APPROVAL);
        Timetable saved = timetableRepository.save(timetable);

        Long currentUserId = SecurityUtil.getAuthenticatedId();
        String period = saved.getAcademicYear() + "·" + saved.getSemester() + "º semestre";

        notificationService.notify(currentUserId, "Horário submetido para aprovação.");
        notificationService.notifyAllWithRole("DIRECTOR",
                "O horário de " + period + " aguarda a sua aprovação.", currentUserId);
        notificationService.notifyAllWithRole("ADMIN",
                "O horário de " + period + " aguarda aprovação.", currentUserId);

        return TimetableResponse.from(saved);
    }

    /**
     * Aproves a timetable if the user deems it valid, and sends a notification to
     * the user approving it and all other users with the role
     * {@link UserRole.ASISTENT}
     *
     * @return {@link Timetable}
     */
    @Transactional
    public TimetableResponse approve(Long id) {
        log.debug("Approving timetable {}", id);
        Timetable timetable = findOrThrow(id);
        timetable.setStatus(TimetableStatus.APPROVED);
        Timetable saved = timetableRepository.save(timetable);

        Long currentUserId = SecurityUtil.getAuthenticatedId();
        notificationService.notify(currentUserId, "Horário aprovado.");
        notificationService.notifyAllWithRole("ASISTENT", "Horário aprovado.", currentUserId);

        return TimetableResponse.from(saved);
    }

    /**
     * Rejects a {@link Timetable} if the user deems it invalid, and sends a
     * notification to the user rejecting it and all other users with the role
     * {@link UserRole.ASISTENT}
     *
     * @return {@link Timetable}
     */
    @Transactional
    public TimetableResponse reject(Long id) {
        log.debug("Rejecting timetable {}", id);
        Timetable timetable = findOrThrow(id);
        timetable.setStatus(TimetableStatus.DRAFT);
        Timetable saved = timetableRepository.save(timetable);

        Long currentUserId = SecurityUtil.getAuthenticatedId();
        notificationService.notify(currentUserId, "Horário rejeitado.");
        notificationService.notifyAllWithRole("ASISTENT", "Horário rejeitado.", currentUserId);

        return TimetableResponse.from(saved);
    }

    @Transactional
    public TimetableResponse publishTimetable(Long id) {
        log.debug("Publishing timetable {}", id);
        Timetable timetable = findOrThrow(id);

        if (timetable.getStatus() == TimetableStatus.PUBLISHED) {
            log.warn("Timetable {} is already published", id);
            throw new IllegalStateException("Timetable is already published");
        }

        if (timetableRepository.countScheduledClasses(id) == 0) {
            log.warn("Cannot publish an empty timetable");
            throw new IllegalStateException(
                    "Cannot publish an empty timetable. Please add time slots first");
        }

        timetable.setStatus(TimetableStatus.PUBLISHED);
        Timetable updated = timetableRepository.save(timetable);

        Long currentUserId = SecurityUtil.getAuthenticatedId();
        String period = updated.getAcademicYear() + "·" + updated.getSemester() + "º semestre";

        notificationService.notify(currentUserId, "Horário publicado com sucesso.");
        notificationService.notifyAllWithRole("ASISTENT",
                "O horário de " + period + " foi publicado.", currentUserId);

        Set<Long> coordinatorIds = timetableRepository.findCoordinatorIdsByTimetableId(id);
        coordinatorIds.forEach(coordinatorId -> notificationService.notify(coordinatorId,
                "O horário da sua turma foi publicado para " + period + "."));

        log.info("Timetable {} published", updated.getId());
        return TimetableResponse.from(updated);
    }

    @Transactional
    public void deleteTimetable(Long id) {
        log.debug("deleting timetable {}", id);
        Timetable timetable = timetableRepository.findById(id)
                .orElseThrow(() -> new TimetableNotFoundException(
                        "Timetable with id %d not found".formatted(id)));

        if (timetable.getStatus() == TimetableStatus.PUBLISHED) {
            log.debug("cannot deleted a published timetable, please archive it first");
            throw new IllegalStateException(
                    "Cannot delete a published timetable. Please archive it first");
        }

        timetableRepository.deleteById(id);
        log.info("deleted timetable {}", id);
    }
}
