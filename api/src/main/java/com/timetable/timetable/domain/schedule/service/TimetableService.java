package com.timetable.timetable.domain.schedule.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.timetable.timetable.domain.schedule.dto.CandidateTeacherResponse;
import com.timetable.timetable.domain.schedule.dto.CreateTimetableRequest;
import com.timetable.timetable.domain.schedule.dto.UpdateTimetableRequest;
import com.timetable.timetable.domain.schedule.entity.AcademicPolicy;
import com.timetable.timetable.domain.schedule.entity.CohortSubject;
import com.timetable.timetable.domain.schedule.entity.Timetable;
import com.timetable.timetable.domain.schedule.entity.TimetableStatus;
import com.timetable.timetable.domain.schedule.exception.TimetableNotFoundException;
import com.timetable.timetable.domain.schedule.repository.TimetableRepository;
import com.timetable.timetable.domain.schedule.repository.CohortSubjectRepository;
import com.timetable.timetable.domain.user.entity.ApplicationUser;
import com.timetable.timetable.domain.user.service.NotificationService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class TimetableService {
    private final TimetableRepository timetableRepository;
    private final CohortSubjectRepository cohortSubjectRepository;
    private final SubjectService subjectService;
    private final NotificationService notificationService;

    @Transactional
    public Timetable createTimetable(CreateTimetableRequest createRequest) {
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
        return saved;
    }

    public Page<Timetable> getAll(Pageable pageable) {
        log.debug("Fetching all timetables");
        return timetableRepository.findAll(pageable);
    }

    public Page<Timetable> getByStatus(TimetableStatus status, Pageable pageable) {
        log.debug("Fetching all {} timetables", status);
        return timetableRepository.findByStatus(status, pageable);
    }

    public Timetable getById(Long id) {
        log.debug("fetching timetable {}", id);
        Timetable timetable = timetableRepository.findById(id)
                .orElseThrow(() -> new TimetableNotFoundException(
                        "Timetable with id %d not found".formatted(id)));

        log.info("timetable {} found", timetable.getId());
        return timetable;
    }

    public Timetable getByAcademicPeriod(int academicYear, int semester) {
        log.debug("fetching timetable by {} year and {} semester", academicYear, semester);
        Timetable timetable = timetableRepository.findByAcademicYearAndSemester(academicYear, semester)
                .orElseThrow(() -> new TimetableNotFoundException(
                        "Timetable for academic period '%s' not found".formatted(academicYear + "." + semester)));

        log.info("timetable {} found", timetable.getId());
        return timetable;
    }

    @Transactional
    public List<CandidateTeacherResponse> getCandidates(Long subjectId, int academicYear, int semester) {
        Set<ApplicationUser> candidates = subjectService.getById(subjectId).getEligibleTeachers();

        List<CandidateTeacherResponse> response = new ArrayList<>();
        for (ApplicationUser c : candidates) {
            List<CohortSubject> cohorts = cohortSubjectRepository
                    .findByAcademicYearAndSemesterAndAssignedTeacher(academicYear, semester, c.getId());
            int weeklyHours = cohorts.size() * AcademicPolicy.WEEKLY_CONTACT_HOURS;
            boolean wouldExceedLimits = (weeklyHours + AcademicPolicy.WEEKLY_CONTACT_HOURS > AcademicPolicy
                    .getWeeklyHoursLimit(c));
            response.add(CandidateTeacherResponse.from(
                    c,
                    weeklyHours,
                    AcademicPolicy.getWeeklyHoursLimit(c),
                    wouldExceedLimits));
        }

        return response;
    }

    @Transactional
    public Timetable updateTimetable(Long id, UpdateTimetableRequest updateRequest) {
        log.debug("updating timetable {}", id);
        Timetable timetable = getById(id);

        // Check if trying to change to a different academic period that already exists
        if (!timetable.getAcademicPeriod().equals(updateRequest.academicYear() + "." + updateRequest.semester()) &&
                timetableRepository.existsByAcademicYearAndSemester(updateRequest.academicYear(),
                        updateRequest.semester())) {
            log.warn("another timetable for {} period already exists", updateRequest.academicYear());
            throw new IllegalArgumentException(
                    "Another timetable for academic period '%s' already exists"
                            .formatted(updateRequest.academicYear()));
        }

        timetable.setAcademicYear(updateRequest.academicYear());
        timetable.setSemester(updateRequest.semester());
        timetable.setStatus(updateRequest.status());

        Timetable updated = timetableRepository.save(timetable);

        log.info("timetable {} updated", updated.getId());
        return updated;
    }

    @Transactional
    public Timetable submitForApproval(Long id) {
        log.debug("Submitting timetable {} for approval", id);
        Timetable timetable = getById(id);
        timetable.setStatus(TimetableStatus.PENDING_APPROVAL);
        Timetable saved = timetableRepository.save(timetable);

        Long currentUserId = com.timetable.timetable.security.SecurityUtil.getAuthenticatedId();
        notificationService.notify(currentUserId, "Horário submetido para aprovação.");
        notificationService.notifyAllWithRole("DIRECTOR",
                "O horário de " + saved.getAcademicYear() + "·" + saved.getSemester()
                        + "º semestre aguarda a sua aprovação.",
                currentUserId);
        notificationService.notifyAllWithRole("ADMIN",
                "O horário de " + saved.getAcademicYear() + "·" + saved.getSemester() + "º semestre aguarda aprovação.",
                currentUserId);

        return saved;
    }

    /**
     * Aproves a timetable if the user deems it valid, and sends a notification to
     * the user approving it and all other users with the role
     * {@link UserRole.ASISTENT}
     *
     * @return {@link Timetable}
     */
    @Transactional
    public Timetable approve(Long id) {
        log.debug("Approving timetable {}", id);
        Timetable timetable = getById(id);
        timetable.setStatus(TimetableStatus.APPROVED);
        Timetable saved = timetableRepository.save(timetable);
        Long currentUserId = com.timetable.timetable.security.SecurityUtil.getAuthenticatedId();
        notificationService.notify(currentUserId, "Horário aprovado.");
        notificationService.notifyAllWithRole("ASISTENT", "Horário aprovado.", currentUserId);

        return saved;
    }

    /**
     * Rejects a {@link Timetable} if the user deems it invalid, and sends a
     * notification to the user rejecting it and all other users with the role
     * {@link UserRole.ASISTENT}
     *
     * @return {@link Timetable}
     */
    @Transactional
    public Timetable reject(Long id) {
        log.debug("Rejecting timetable {}", id);
        Timetable timetable = getById(id);
        timetable.setStatus(TimetableStatus.DRAFT);
        Timetable saved = timetableRepository.save(timetable);

        Long currentUserId = com.timetable.timetable.security.SecurityUtil.getAuthenticatedId();
        notificationService.notify(currentUserId, "Horário rejeitado.");
        notificationService.notifyAllWithRole("ASISTENT", "Horário rejeitado.", currentUserId);

        return saved;
    }

    @Transactional
    public Timetable publishTimetable(Long id) {
        log.debug("publishing timetable");
        Timetable timetable = timetableRepository.findById(id)
                .orElseThrow(() -> new TimetableNotFoundException(
                        "Timetable with id %d not found".formatted(id)));

        if (timetable.getStatus() == TimetableStatus.PUBLISHED) {
            log.warn("timetable {} is already published", id);
            throw new IllegalStateException(
                    "Timetable is already published");
        }

        if (timetable.getScheduledClasses() == null || timetable.getScheduledClasses().isEmpty()) {
            log.warn("cannot publish an empty timetable");
            throw new IllegalStateException(
                    "Cannot publish an empty timetable. Please add time slots first");
        }

        timetable.setStatus(TimetableStatus.PUBLISHED);
        Timetable updated = timetableRepository.save(timetable);

        Long currentUserId = com.timetable.timetable.security.SecurityUtil.getAuthenticatedId();
        notificationService.notify(currentUserId, "Horário publicado com sucesso.");
        notificationService.notifyAllWithRole("ASISTENT",
                "O horário de " + updated.getAcademicYear() + "·" + updated.getSemester() + "º semestre foi publicado.",
                currentUserId);

        java.util.Set<Long> coordinatorIds = updated.getScheduledClasses().stream()
                .map(sc -> sc.getCohortSubject().getCohort().getCourse().getCoordinator())
                .filter(java.util.Objects::nonNull)
                .map(com.timetable.timetable.domain.user.entity.ApplicationUser::getId)
                .collect(java.util.stream.Collectors.toSet());

        for (Long coordinatorId : coordinatorIds) {
            notificationService.notify(coordinatorId,
                    "O horário da sua turma foi publicado para " + updated.getAcademicYear() + "·"
                            + updated.getSemester()
                            + "º semestre.");
        }

        log.info("timetable {} updated", updated.getId());
        return updated;
    }

    @Transactional
    public Timetable archiveTimetable(Long id) {
        log.debug("archiving timetable {}", id);
        Timetable timetable = timetableRepository.findById(id)
                .orElseThrow(() -> new TimetableNotFoundException(
                        "Timetable with id %d not found".formatted(id)));

        if (timetable.getStatus() == TimetableStatus.ARCHIVED) {
            log.warn("timetable {} already arquived", timetable.getId());
            throw new IllegalStateException(
                    "Timetable is already archived");
        }

        timetable.setStatus(TimetableStatus.ARCHIVED);
        Timetable updated = timetableRepository.save(timetable);

        log.info("timetable {} arquived", timetable.getId());
        return updated;
    }

    @Transactional
    public Timetable revertToDraft(Long id) {
        log.debug("Reverting timetable {} to draft", id);
        Timetable timetable = timetableRepository.findById(id)
                .orElseThrow(() -> new TimetableNotFoundException(
                        "Timetable with id %d not found".formatted(id)));

        if (timetable.getStatus() == TimetableStatus.DRAFT) {
            log.debug("timetable {} is already in draft status", timetable.getId());
            throw new IllegalStateException(
                    "Timetable is already in draft status");
        }

        if (timetable.getStatus() == TimetableStatus.ARCHIVED) {
            log.debug("cannot revert an archived timetable");
            throw new IllegalStateException(
                    "Cannot revert an archived timetable to draft. Please create a new timetable");
        }

        timetable.setStatus(TimetableStatus.DRAFT);
        Timetable updated = timetableRepository.save(timetable);

        log.info("timetable {} updated", updated.getId());
        return updated;
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
