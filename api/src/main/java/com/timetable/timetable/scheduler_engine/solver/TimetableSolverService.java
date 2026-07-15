package com.timetable.timetable.scheduler_engine.solver;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.springframework.stereotype.Service;
import ai.timefold.solver.core.api.solver.SolverManager;
import ai.timefold.solver.core.api.solver.SolverJob;
import ai.timefold.solver.core.api.solver.SolverStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import com.timetable.timetable.domain.schedule.dto.GenerationStartResult;
import com.timetable.timetable.domain.schedule.dto.PreSolverRequest;
import com.timetable.timetable.domain.schedule.dto.PreSolverResult;
import com.timetable.timetable.scheduler_engine.domain.TimetableSolution;
import com.timetable.timetable.scheduler_engine.preparation.PreSolverService;

@Service
@RequiredArgsConstructor
@Slf4j
public class TimetableSolverService {

    private final SolverManager<TimetableSolution, UUID> solverManager;
    private final Map<UUID, SolverJob<TimetableSolution, UUID>> jobs = new ConcurrentHashMap<>();

    private final PreSolverService preSolverService;
    private final TimetableGeneratorService timetableGeneratorService;
    private final TimetablePersistenceService persistenceService; // ← NEW
    private final ai.timefold.solver.core.api.solver.SolutionManager<TimetableSolution, ai.timefold.solver.core.api.score.buildin.hardsoft.HardSoftScore> solutionManager;

    private final ExecutorService asyncExecutor = Executors.newCachedThreadPool();

    /**
     * Test only — raw JSON input, no persistence
     */
    public UUID startSolverJob(TimetableSolution problem) {
        UUID jobId = UUID.randomUUID();
        log.info("Starting test solver job with ID: {}", jobId);
        jobs.put(jobId, solverManager.solve(jobId, problem));
        return jobId;
    }

    /**
     * Called repeatedly by the frontend until the solver finishes.
     *
     * When the solver is done (NOT_SOLVING):
     * 1. Retrieves the best solution from the job
     * 2. Persists it to Timetable + ScheduledClass via TimetablePersistenceService
     * 3. Removes the job from memory
     * 4. Returns the solution so the controller can forward it (or just the
     * score/feasibility)
     *
     * Returns null while still solving or if jobId is unknown.
     */
    public TimetableSolution getSolution(UUID jobId) {
        SolverJob<TimetableSolution, UUID> job = jobs.get(jobId);
        if (job == null) {
            log.warn("Job {} not found", jobId);
            return null;
        }

        if (job.getSolverStatus() != SolverStatus.NOT_SOLVING) {
            return null;
        }

        try {
            TimetableSolution solution = job.getFinalBestSolution();
            log.info("Solver done for job {}. Score: {} | Feasible: {} | Unassigned: {}",
                    jobId, solution.getScore(), solution.isFeasible(), solution.getUnassignedLessons());

            // TEMPORARY — remove after presentation
            if (!solution.isFeasible()) {
                solutionManager.explain(solution)
                        .getConstraintMatchTotalMap()
                        .forEach((name, match) -> {
                            if (match.getScore().hardScore() < 0) {
                                log.error("HC VIOLATION: {} | occurrences={} | score={}",
                                        name,
                                        match.getConstraintMatchCount(),
                                        match.getScore());
                            }
                        });
            }

            try {
                persistenceService.saveSolution(solution);
            } catch (Exception e) {
                log.error("Failed to persist solution for job {}: {}", jobId, e.getMessage(), e);
            }

            jobs.remove(jobId);
            return solution;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Thread interrupted retrieving solution for job {}", jobId, e);
        } catch (ExecutionException e) {
            log.error("Error retrieving solution for job {}", jobId, e);
        }

        return null;
    }

    /**
     * Async prepare + generate
     */
    public GenerationStartResult prepareAndGenerateAsync(
            int academicYear,
            int semester,
            PreSolverRequest prepRequest) {

        UUID jobId = UUID.randomUUID();
        log.info("Starting ASYNC prep + generation for job {}", jobId);

        asyncExecutor.submit(() -> {
            try {
                preSolverService.prepare(prepRequest);
                log.info("Pre-solver done for job {}. Starting solver...", jobId);
                timetableGeneratorService.generateTimetable(academicYear, semester, jobId, jobs);
            } catch (Exception e) {
                log.error("Error in async prep for job {}: {}", jobId, e.getMessage(), e);
            }
        });

        return new GenerationStartResult(
                jobId,
                new PreSolverResult(0, 0, 0, List.of("preparation running in the background")));
    }

    public SolverStatus getStatus(UUID jobId) {
        SolverJob<TimetableSolution, UUID> job = jobs.get(jobId);
        return job != null ? job.getSolverStatus() : null;
    }

    public void terminateEarly(UUID jobId) {
        log.info("Terminating solver job {} early", jobId);
        solverManager.terminateEarly(jobId);
        jobs.remove(jobId);
    }
}
