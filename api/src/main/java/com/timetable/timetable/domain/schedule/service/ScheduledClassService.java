package com.timetable.timetable.domain.schedule.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.timetable.timetable.domain.schedule.dto.CreateScheduledClassRequest;
import com.timetable.timetable.domain.schedule.dto.UpdateScheduledClassRequest;
import com.timetable.timetable.domain.schedule.entity.*;
import com.timetable.timetable.domain.schedule.exception.ScheduledClassNotFoundException;
import com.timetable.timetable.domain.schedule.repository.ScheduledClassRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class ScheduledClassService {

    private final ScheduledClassRepository scheduledClassRepository;
    private final CohortSubjectService cohortSubjectService;
    private final TimetableService timetableService;
    private final RoomService roomService;
    private final TimeslotService timeslotService;

    @Transactional
    public ScheduledClass createScheduledClass(CreateScheduledClassRequest request) {
        log.debug("Creating scheduled class");

        CohortSubject cohortSubject =
                cohortSubjectService.getById(request.cohortSubjectId());
        Room room =
                roomService.findOrThrow(request.roomId());
        Timeslot timeslot =
                timeslotService.getById(request.timeslotId());

        Timetable timetable = request.timetableId() != null
                ? timetableService.getById(request.timetableId())
                : null;

        validateCohortSubject(cohortSubject);

        checkForConflicts(
                null,
                cohortSubject,
                room,
                timeslot,
                timetable
        );

        ScheduledClass scheduledClass = ScheduledClass.builder()
                .cohortSubject(cohortSubject)
                .room(room)
                .timeslot(timeslot)
                .timetable(timetable)
                .build();

        ScheduledClass saved = scheduledClassRepository.save(scheduledClass);

        log.info(
                "Scheduled class {} created for {} ({})",
                saved.getId(),
                cohortSubject.getDisplayName(),
                timeslot.getDisplayName()
        );

        return saved;
    }

    @Transactional(readOnly = true)
    public Page<ScheduledClass> getAll(Pageable pageable) {
        return scheduledClassRepository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public ScheduledClass getById(Long id) {
        return scheduledClassRepository.findByIdWithDetails(id)
                .orElseThrow(() ->
                        new ScheduledClassNotFoundException(
                                "Scheduled class with id %d not found".formatted(id)
                        )
                );
    }

    @Transactional(readOnly = true)
    public Page<ScheduledClass> getByTimetable(Long timetableId, Pageable pageable) {
        Timetable timetable = timetableService.getById(timetableId);
        return scheduledClassRepository.findByTimetable(timetable, pageable);
    }

    @Transactional(readOnly = true)
    public Page<ScheduledClass> getByCohort(Long cohortId, Pageable pageable) {
        return scheduledClassRepository.findByCohortId(cohortId, pageable);
    }

    @Transactional(readOnly = true)
    public Page<ScheduledClass> getByTeacher(Long teacherId, Pageable pageable) {
        return scheduledClassRepository.findByTeacherId(teacherId, pageable);
    }

    @Transactional(readOnly = true)
    public Page<ScheduledClass> getByCohortSubject(Long cohortSubjectId, Pageable pageable) {
        return scheduledClassRepository.findByCohortSubjectId(cohortSubjectId, pageable);
    }

    @Transactional
    public ScheduledClass updateScheduledClass(
            Long id,
            UpdateScheduledClassRequest request
    ) {
        log.debug("Updating scheduled class {}", id);

        ScheduledClass scheduledClass = getById(id);

        CohortSubject cohortSubject = resolveCohortSubject(
                scheduledClass, request.cohortSubjectId()
        );
        Room room = resolveRoom(
                scheduledClass, request.roomId()
        );
        Timeslot timeslot = resolveTimeslot(
                scheduledClass, request.timeslotId()
        );
        Timetable timetable = resolveTimetable(
                scheduledClass, request.timetableId()
        );

        validateCohortSubject(cohortSubject);

        checkForConflicts(
                id,
                cohortSubject,
                room,
                timeslot,
                timetable
        );

        scheduledClass.setCohortSubject(cohortSubject);
        scheduledClass.setRoom(room);
        scheduledClass.setTimeslot(timeslot);
        scheduledClass.setTimetable(timetable);

        ScheduledClass updated = scheduledClassRepository.save(scheduledClass);

        log.info("Scheduled class {} updated", updated.getId());
        return updated;
    }

    @Transactional
    public void deleteScheduledClass(Long id) {
        if (!scheduledClassRepository.existsById(id)) {
            throw new ScheduledClassNotFoundException(
                    "Scheduled class with id %d not found".formatted(id)
            );
        }
        scheduledClassRepository.deleteById(id);
        log.info("Scheduled class {} deleted", id);
    }

    private void validateCohortSubject(CohortSubject cohortSubject) {
        if (!cohortSubject.isActive()) {
            throw new IllegalStateException(
                    "Cannot schedule a class for an inactive cohort subject"
            );
        }
    }

    private void checkForConflicts(
            Long excludeId,
            CohortSubject cohortSubject,
            Room room,
            Timeslot timeslot,
            Timetable timetable
    ) {
        List<ScheduledClass> conflicts =
                scheduledClassRepository.findConflicts(
                        cohortSubject.getAssignedTeacher(),
                        cohortSubject.getCohort(),
                        room,
                        timeslot,
                        timetable
                );

        if (excludeId != null) {
            conflicts.removeIf(sc -> sc.getId().equals(excludeId));
        }

        if (!conflicts.isEmpty()) {
            throw new IllegalArgumentException(
                    "Schedule conflict detected for timeslot " +
                            timeslot.getDisplayName()
            );
        }
    }

    private CohortSubject resolveCohortSubject(
            ScheduledClass sc,
            Long newId
    ) {
        return newId.equals(sc.getCohortSubject().getId())
                ? sc.getCohortSubject()
                : cohortSubjectService.getById(newId);
    }

    private Room resolveRoom(
            ScheduledClass sc,
            Long newId
    ) {
        return newId.equals(sc.getRoom().getId())
                ? sc.getRoom()
                : roomService.findOrThrow(newId);
    }

    private Timeslot resolveTimeslot(
            ScheduledClass sc,
            Long newId
    ) {
        return newId.equals(sc.getTimeslot().getId())
                ? sc.getTimeslot()
                : timeslotService.getById(newId);
    }

    private Timetable resolveTimetable(
            ScheduledClass sc,
            Long newId
    ) {
        if (newId == null) {
            return null;
        }
        if (sc.getTimetable() != null &&
            newId.equals(sc.getTimetable().getId())) {
            return sc.getTimetable();
        }
        return timetableService.getById(newId);
    }

    @Transactional(readOnly = true)
    public int countScheduledClassesForCohortSubject(Long cohortSubjectId) {
        return scheduledClassRepository.countByCohortSubjectId(cohortSubjectId);
    }
}
