package com.timetable.timetable.scheduler_engine.solver;

import ai.timefold.solver.core.api.solver.SolverJob;
import ai.timefold.solver.core.api.solver.SolverManager;
import com.timetable.timetable.domain.schedule.entity.CohortSubject;
import com.timetable.timetable.domain.schedule.entity.Room;
import com.timetable.timetable.domain.schedule.entity.ScheduledClass;
import com.timetable.timetable.domain.schedule.entity.Timeslot;
import com.timetable.timetable.domain.schedule.entity.Timetable;
import com.timetable.timetable.domain.schedule.repository.CohortSubjectRepository;
import com.timetable.timetable.domain.schedule.repository.RoomRepository;
import com.timetable.timetable.domain.schedule.repository.ScheduledClassRepository;
import com.timetable.timetable.domain.schedule.repository.TimeslotRepository;
import com.timetable.timetable.domain.schedule.repository.TimetableRepository;
import com.timetable.timetable.scheduler_engine.domain.LessonAssignment;
import com.timetable.timetable.scheduler_engine.domain.TimetableSolution;
import com.timetable.timetable.scheduler_engine.domain.info.RoomInfo;
import com.timetable.timetable.scheduler_engine.domain.info.TimeslotInfo;
import com.timetable.timetable.scheduler_engine.mapper.TimetableSolutionMapper;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class TimetableGeneratorService {

  private final CohortSubjectRepository cohortSubjectRepository;
  private final RoomRepository roomRepository;
  private final TimeslotRepository timeslotRepository;
  private final TimetableRepository timetableRepository;
  private final ScheduledClassRepository scheduledClassRepository;

  private final TimetableSolutionMapper solutionMapper;
  private final SolverManager<TimetableSolution, UUID> solverManager;

  @org.springframework.transaction.annotation.Transactional(readOnly = true)
  public UUID generateTimetable(
      int academicYear,
      int semester,
      UUID jobId,
      Map<UUID, SolverJob<TimetableSolution, UUID>> jobs) {

    log.info("Starting solver for job: {}", jobId);

    List<CohortSubject> allActive =
        cohortSubjectRepository.findByAcademicYearAndSemesterAndIsActive(
            academicYear, semester, true);
    List<Timeslot> timeslots = timeslotRepository.findAll();
    List<Room> rooms = roomRepository.findAll();

    if (allActive.isEmpty())
      throw new IllegalStateException(
          "No active cohort-subjects found for " + academicYear + "." + semester);
    if (timeslots.isEmpty()) throw new IllegalStateException("No timeslots available");
    if (rooms.isEmpty()) throw new IllegalStateException("No rooms available");

    // Carrega os ScheduledClass já pinned (pré-atribuídos pela Simulação
    // Empresarial)
    Timetable timetable =
        timetableRepository.findByAcademicYearAndSemester(academicYear, semester).orElse(null);

    List<ScheduledClass> pinnedClasses =
        timetable != null
            ? scheduledClassRepository.findByTimetableAndPinnedTrue(timetable)
            : List.of();

    // Exclui os CohortSubjects que já têm ScheduledClass pinned
    Set<Long> pinnedCohortSubjectIds =
        pinnedClasses.stream()
            .map(sc -> sc.getCohortSubject().getId())
            .collect(java.util.stream.Collectors.toSet());

    List<CohortSubject> unplanned =
        allActive.stream().filter(cs -> !pinnedCohortSubjectIds.contains(cs.getId())).toList();

    log.info(
        "CohortSubjects: {} total, {} pinned, {} to solve",
        allActive.size(),
        pinnedClasses.size(),
        unplanned.size());

    // Constrói o problema só com os não-pinned
    TimetableSolution problem =
        solutionMapper.toPlanningProblem(unplanned, timeslots, rooms, academicYear, semester);

    // Injeta os pinned como LessonAssignments já fixos
    if (!pinnedClasses.isEmpty()) {
      Map<Long, TimeslotInfo> timeslotById =
          problem.getAvailableTimeslots().stream()
              .collect(
                  java.util.stream.Collectors.toMap(
                      TimeslotInfo::getId, java.util.function.Function.identity()));
      Map<Long, RoomInfo> roomById =
          problem.getAvailableRooms().stream()
              .collect(
                  java.util.stream.Collectors.toMap(
                      RoomInfo::getId, java.util.function.Function.identity()));

      for (ScheduledClass sc : pinnedClasses) {
        TimeslotInfo timeslot = timeslotById.get(sc.getTimeslot().getId());
        RoomInfo room = roomById.get(sc.getRoom().getId());

        if (timeslot == null || room == null) {
          log.warn("Pinned ScheduledClass id={} has invalid timeslot/room — skipping", sc.getId());
          continue;
        }

        problem
            .getLessonAssignments()
            .add(
                LessonAssignment.builder()
                    .id(-sc.getId())
                    .cohortSubject(solutionMapper.toCohortSubjectInfo(sc.getCohortSubject()))
                    .blockNumber(0)
                    .timeslot(timeslot)
                    .room(room)
                    .pinned(true)
                    .build());
      }

      log.info("Injected {} pinned LessonAssignments into planning problem", pinnedClasses.size());
    }

    SolverJob<TimetableSolution, UUID> job = solverManager.solve(jobId, problem);
    jobs.put(jobId, job);
    return jobId;
  }
}
