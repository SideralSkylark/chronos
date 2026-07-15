package com.timetable.timetable.scheduler_engine.solver;

import ai.timefold.solver.core.api.solver.SolverStatus;
import com.timetable.timetable.domain.schedule.dto.CohortEstimationConfig;
import com.timetable.timetable.domain.schedule.dto.GenerationStartResult;
import com.timetable.timetable.domain.schedule.dto.PhantomTeacherPolicy;
import com.timetable.timetable.domain.schedule.dto.PreSolverRequest;
import com.timetable.timetable.scheduler_engine.domain.TimetableSolution;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@Slf4j
@RequestMapping("/api/v1/solver")
@RequiredArgsConstructor
public class TimetableSolverController {

  private final TimetableSolverService solverService;

  /**
   * Start a solver job with a manually created problem.
   *
   * @param problem The timetabling problem to solve
   * @return UUID of the solver job (use this to poll for results)
   */
  @PostMapping("/test")
  public ResponseEntity<JobStartedResponse> startTestJob(@RequestBody TimetableSolution problem) {
    log.info(
        "Received test problem with {} lessons, {} timeslots, {} rooms",
        problem.getTotalLessons(),
        problem.getAvailableTimeslots() != null ? problem.getAvailableTimeslots().size() : 0,
        problem.getAvailableRooms() != null ? problem.getAvailableRooms().size() : 0);

    UUID jobId = solverService.startSolverJob(problem);

    return ResponseEntity.accepted()
        .body(
            new JobStartedResponse(
                jobId,
                "Solver job started. Poll GET /api/v1/solver" + jobId + " for results.",
                problem.getTotalLessons()));
  }

  /** Preparação + Geração TOTALMENTE async Retorna UUID imediatamente (<50ms) */
  @PostMapping("/generate")
  public ResponseEntity<GenerationStartedResponse> generate(@RequestBody GenerateRequest request) {

    PreSolverRequest prepRequest =
        PreSolverRequest.custom(
            request.academicYear(),
            request.semester(),
            CohortEstimationConfig.defaultConfig(),
            PhantomTeacherPolicy.CREATE_IF_NEEDED);

    GenerationStartResult result =
        solverService.prepareAndGenerateAsync(
            request.academicYear(), request.semester(), prepRequest);

    return ResponseEntity.accepted()
        .body(
            new GenerationStartedResponse(
                result.jobId(),
                "Preparation and generation started in background. Poll /status/"
                    + result.jobId()));
  }

  /**
   * Get the solution for a solver job (if ready).
   *
   * <p>Returns: - 202 Accepted - Still solving (poll again) - 200 OK - Solution ready (with JSON
   * body) - 404 Not Found - Job ID doesn't exist
   *
   * @param jobId The job ID from the start request
   * @return The solution (or 202 if still solving)
   */
  @GetMapping("/{jobId}")
  public ResponseEntity<?> getSolution(@PathVariable UUID jobId) {
    TimetableSolution solution = solverService.getSolution(jobId);

    if (solution == null) {
      SolverStatus status = solverService.getStatus(jobId);

      if (status == null) {
        return ResponseEntity.notFound().build();
      }

      return ResponseEntity.accepted()
          .body(new JobStatusResponse(jobId, status.toString(), null, null));
    }

    log.info(
        "Returning solution for job {}. Score: {}, Feasible: {}",
        jobId,
        solution.getScore(),
        solution.isFeasible());

    return ResponseEntity.ok(solution);
  }

  /**
   * Get the current status of a solver job.
   *
   * @param jobId The job ID
   * @return Status information
   */
  @GetMapping("/{jobId}/status")
  public ResponseEntity<JobStatusResponse> getStatus(@PathVariable UUID jobId) {
    SolverStatus status = solverService.getStatus(jobId);

    if (status == null) {
      return ResponseEntity.notFound().build();
    }

    return ResponseEntity.ok(
        new JobStatusResponse(
            jobId,
            status.toString(),
            null,
            "Use GET /api/v1/solver/test/"
                + jobId
                + " to retrieve the solution when status is NOT_SOLVING"));
  }

  /**
   * Terminate a solver job before it finishes naturally. Useful if you realize the problem is too
   * large or want to stop early.
   *
   * @param jobId The job ID to terminate
   * @return Confirmation message
   */
  @PostMapping("/{jobId}/terminate")
  public ResponseEntity<String> terminateJob(@PathVariable UUID jobId) {
    SolverStatus status = solverService.getStatus(jobId);

    if (status == null) {
      return ResponseEntity.notFound().build();
    }

    if (status == SolverStatus.NOT_SOLVING) {
      return ResponseEntity.badRequest()
          .body("Job " + jobId + " is already finished (status: " + status + ")");
    }

    solverService.terminateEarly(jobId);

    return ResponseEntity.ok("Job " + jobId + " terminated successfully");
  }

  // ========================================
  // RESPONSE DTOs
  // ========================================

  record JobStartedResponse(UUID jobId, String message, int totalLessonsToSchedule) {}

  record GenerationStartedResponse(UUID jobId, String message) {}

  record GenerateRequest(int academicYear, int semester) {}

  record JobStatusResponse(UUID jobId, String status, String score, String message) {}
}
