package com.timetable.timetable.scheduler_engine.solver;

import com.timetable.timetable.domain.schedule.entity.*;
import com.timetable.timetable.domain.schedule.repository.ScheduledClassRepository;
import com.timetable.timetable.domain.schedule.repository.TimetableRepository;
import com.timetable.timetable.scheduler_engine.domain.TimetableSolution;
import com.timetable.timetable.scheduler_engine.mapper.PersistenceMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Persists a solved TimetableSolution into Timetable + ScheduledClass.
 * Delegates entity resolution to the existing PersistenceMapper.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TimetablePersistenceService {

    private final TimetableRepository timetableRepository;
    private final PersistenceMapper persistenceMapper;
    private final ScheduledClassRepository scheduledClassRepository;
    private final com.timetable.timetable.domain.user.service.NotificationService notificationService;

    @Transactional
    public Timetable saveSolution(TimetableSolution solution) {
        int year = solution.getAcademicYear();
        int semester = solution.getSemester();

        log.info("Persisting timetable {}.{} — {} lessons ({} unassigned)",
                year, semester, solution.getTotalLessons(), solution.getUnassignedLessons());

        // Reutiliza o timetable existente (criado pelo PreSolverService)
        // ou cria um novo se não existir
        Timetable timetable = timetableRepository
                .findByAcademicYearAndSemester(year, semester)
                .orElseGet(() -> timetableRepository.save(
                        Timetable.builder()
                                .academicYear(year)
                                .semester(semester)
                                .status(TimetableStatus.DRAFT)
                                .build()));

        // Apaga apenas os ScheduledClass NÃO pinned — preserva os pinned da Simulação
        scheduledClassRepository.deleteByTimetableAndPinnedFalse(timetable);

        // Converte APENAS os não-pinned — os pinned já estão na DB
        List<ScheduledClass> classes = persistenceMapper.convertToScheduledClasses(
                solution, timetable);

        scheduledClassRepository.saveAll(classes);

        timetable.setStatus(TimetableStatus.DRAFT);
        timetable.setScore(solution.getScore().toString());
        timetable.setFeasible(solution.isFeasible());
        Timetable saved = timetableRepository.save(timetable);

        try {
            Long currentUserId = com.timetable.timetable.security.SecurityUtil.getAuthenticatedId();
            notificationService.notify(currentUserId,
                    "Horário " + year + "·" + semester + "º semestre gerado com sucesso.");
            notificationService.notifyAllWithRole("DIRECTOR",
                    "Um novo horário foi gerado para " + year + "·" + semester + "º semestre.",
                    currentUserId);
            notificationService.notifyAllWithRole("ADMIN",
                    "Um novo horário foi gerado para " + year + "·" + semester + "º semestre.",
                    currentUserId);
        } catch (Exception e) {
            log.warn("Failed to send generation notifications: {}", e.getMessage());
        }

        log.info("Saved timetable id={} for {}.{} with {} scheduled classes",
                saved.getId(), year, semester, classes.size());
        return saved;
    }
}
