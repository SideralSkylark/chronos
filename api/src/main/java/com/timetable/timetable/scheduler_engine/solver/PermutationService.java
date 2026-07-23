package com.timetable.timetable.scheduler_engine.solver;

import ai.timefold.solver.core.api.score.buildin.hardsoft.HardSoftScore;
import ai.timefold.solver.core.api.solver.SolutionManager;
import com.timetable.timetable.domain.schedule.entity.Room;
import com.timetable.timetable.domain.schedule.entity.ScheduledClass;
import com.timetable.timetable.domain.schedule.entity.Timeslot;
import com.timetable.timetable.domain.schedule.repository.RoomRepository;
import com.timetable.timetable.domain.schedule.repository.ScheduledClassRepository;
import com.timetable.timetable.domain.schedule.repository.TimeslotRepository;
import com.timetable.timetable.scheduler_engine.domain.LessonAssignment;
import com.timetable.timetable.scheduler_engine.domain.TimetableSolution;
import com.timetable.timetable.scheduler_engine.domain.info.RoomInfo;
import com.timetable.timetable.scheduler_engine.domain.info.TimeslotInfo;
import com.timetable.timetable.scheduler_engine.mapper.TimetableSolutionMapper;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PermutationService {

  private final ScheduledClassRepository scheduledClassRepository;
  private final TimeslotRepository timeslotRepository;
  private final RoomRepository roomRepository;
  private final TimetableSolutionMapper solutionMapper;
  private final SolutionManager<TimetableSolution, HardSoftScore> solutionManager;

  // ========================================
  // FIND VALID SLOTS
  // ========================================

  @Transactional(readOnly = true)
  public List<ValidSlotResponse> findValidSlots(
      Long scheduledClassId, int academicYear, int semester) {

    List<ScheduledClass> allClasses = scheduledClassRepository.findAllWithDetailsByPeriod(academicYear, semester);

    if (allClasses.isEmpty()) {
      throw new IllegalStateException(
          "No persisted timetable for %d.%d".formatted(academicYear, semester));
    }

    ScheduledClass target = allClasses.stream()
        .filter(sc -> sc.getId().equals(scheduledClassId))
        .findFirst()
        .orElseThrow(
            () -> new IllegalArgumentException("ScheduledClass not found: " + scheduledClassId));

    Long targetCohortId = target.getCohort().getId();

    TimetableSolution solution = solutionMapper.fromScheduledClasses(
        allClasses,
        timeslotRepository.findAll(),
        roomRepository.findAll(),
        academicYear,
        semester);

    // ── Diagnostic ────────────────────────────────────────────────────────
    solutionManager.update(solution);
    HardSoftScore initialScore = solution.getScore();
    log.info("[DIAG] Initial score of rebuilt solution: {}", initialScore);
    if (initialScore != null && initialScore.hardScore() != 0) {
      log.warn(
          "[DIAG] Rebuilt solution already has hard violations ({})! "
              + "Object identity problem in fromScheduledClasses — planning variables "
              + "are not the same instances as those in the value range.",
          initialScore);
    }

    // ── Occupants map (only lessons from other cohorts) ────────────────────
    Map<Long, List<LessonAssignment>> occupantsByTimeslotId = solution.getLessonAssignments().stream()
        .filter(la -> !la.getId().equals(scheduledClassId))
        .filter(la -> la.getTimeslot() != null)
        .filter(la -> !la.getCohortSubject().getCohort().getId().equals(targetCohortId))
        .collect(Collectors.groupingBy(la -> la.getTimeslot().getId()));

    LessonAssignment targetLesson = solution.getLessonAssignments().stream()
        .filter(la -> la.getId().equals(scheduledClassId))
        .findFirst()
        .orElseThrow();

    // ── Optional pair detection ────────────────────────────────────────────
    // Find the pair that shares both the same optionalGroupId AND the same
    // blockNumber — this ensures bloco1 pairs with bloco1, bloco2 with bloco2.
    final LessonAssignment optionalPair;
    if (targetLesson.getOptionalGroupId() != null) {
      optionalPair = solution.getLessonAssignments().stream()
          .filter(la -> !la.getId().equals(scheduledClassId))
          .filter(la -> targetLesson.getOptionalGroupId().equals(la.getOptionalGroupId()))
          .filter(la -> la.getBlockNumber() == targetLesson.getBlockNumber())
          .findFirst()
          .orElse(null);
    } else {
      optionalPair = null;
    }

    int cohortYear = target.getCohort().getYear();
    TimeslotInfo originalTimeslot = targetLesson.getTimeslot();
    RoomInfo originalRoom = targetLesson.getRoom();

    final TimeslotInfo pairOriginalTimeslot = optionalPair != null ? optionalPair.getTimeslot() : null;

    List<TimeslotInfo> candidates = solution.getAvailableTimeslots().stream()
        .filter(ts -> isCorrectPeriod(ts, cohortYear))
        .filter(ts -> !ts.getId().equals(originalTimeslot.getId()))
        .toList();

    log.info(
        "Evaluating {} candidate slots for ScheduledClass {}", candidates.size(), scheduledClassId);

    List<ValidSlotResponse> valid = new ArrayList<>();

    for (TimeslotInfo candidate : candidates) {
      List<LessonAssignment> occupants = occupantsByTimeslotId.getOrDefault(candidate.getId(), List.of());

      LessonAssignment occupant = occupants.size() == 1 ? occupants.get(0) : null;

      for (RoomInfo candidateRoom : solution.getAvailableRooms()) {

        if (!candidateRoom.hasSufficientCapacity(targetLesson.getStudentCount()))
          continue;
        if (!candidateRoom.isAvailableForCourse(targetLesson.getCourseId(), candidate.getPeriod()))
          continue;

        // ── Apply tentative move ───────────────────────────────────────────
        targetLesson.setTimeslot(candidate);
        targetLesson.setRoom(candidateRoom);
        if (optionalPair != null)
          optionalPair.setTimeslot(candidate);
        if (occupant != null)
          occupant.setTimeslot(originalTimeslot);

        try {
          solutionManager.update(solution);
          HardSoftScore score = solution.getScore();

          if (score != null && score.hardScore() == 0) {
            if (occupant != null) {
              ScheduledClass occupantSc = allClasses.stream()
                  .filter(sc -> sc.getId().equals(occupant.getId()))
                  .findFirst()
                  .orElse(null);

              valid.add(
                  ValidSlotResponse.swap(
                      candidate.getId(),
                      candidate.getDayOfWeek().toString(),
                      candidate.getStartTime().toString(),
                      candidate.getEndTime().toString(),
                      occupant.getId(),
                      occupantSc != null ? occupantSc.getSubject().getName() : "?",
                      occupantSc != null ? occupantSc.getCohort().getDisplayName() : "?",
                      candidateRoom.getName(),
                      candidateRoom.getId()));
            } else {
              valid.add(
                  ValidSlotResponse.empty(
                      candidate.getId(),
                      candidate.getDayOfWeek().toString(),
                      candidate.getStartTime().toString(),
                      candidate.getEndTime().toString(),
                      candidateRoom.getName(),
                      candidateRoom.getId()));
            }
            break; // First valid room is enough for this slot
          }
        } finally {
          // ── Restore state ──────────────────────────────────────────────
          targetLesson.setTimeslot(originalTimeslot);
          targetLesson.setRoom(originalRoom);
          if (optionalPair != null)
            optionalPair.setTimeslot(pairOriginalTimeslot);
          if (occupant != null)
            occupant.setTimeslot(candidate);
        }
      }
    }

    log.info(
        "ScheduledClass {} → {}/{} valid permutations",
        scheduledClassId,
        valid.size(),
        candidates.size());
    return valid;
  }

  // ========================================
  // APPLY SWAP
  // ========================================

  @Transactional
  public void applySwap(
      Long scheduledClassId, Long targetTimeslotId, Long targetRoomId, Long swapWithId) {

    ScheduledClass scX = scheduledClassRepository
        .findById(scheduledClassId)
        .orElseThrow(
            () -> new IllegalArgumentException("ScheduledClass not found: " + scheduledClassId));

    Timeslot newTimeslot = timeslotRepository
        .findById(targetTimeslotId)
        .orElseThrow(
            () -> new IllegalArgumentException("Timeslot not found: " + targetTimeslotId));

    Room newRoom = roomRepository
        .findById(targetRoomId)
        .orElseThrow(() -> new IllegalArgumentException("Room not found: " + targetRoomId));

    Timeslot xOriginal = scX.getTimeslot(); // capture before mutation

    if (swapWithId != null) {
      ScheduledClass scY = scheduledClassRepository
          .findById(swapWithId)
          .orElseThrow(
              () -> new IllegalArgumentException("ScheduledClass not found: " + swapWithId));

      Timeslot yOriginal = scY.getTimeslot(); // capture before mutation

      scX.setTimeslot(newTimeslot);
      scX.setRoom(newRoom);
      scY.setTimeslot(xOriginal);

      moveOptionalPairIfPresent(scX, scheduledClassId, swapWithId, xOriginal, newTimeslot);
      moveOptionalPairIfPresent(scY, swapWithId, scheduledClassId, yOriginal, xOriginal);

      log.info("Full swap: ScheduledClass {} ↔ ScheduledClass {}", scheduledClassId, swapWithId);
    } else {
      scX.setTimeslot(newTimeslot);
      scX.setRoom(newRoom);

      moveOptionalPairIfPresent(scX, scheduledClassId, null, xOriginal, newTimeslot);

      log.info(
          "Move: ScheduledClass {} → Timeslot {} Room {}",
          scheduledClassId,
          targetTimeslotId,
          targetRoomId);
    }
  }

  // ========================================
  // FIND COHORT SWAP CANDIDATES
  // ========================================

  @Transactional(readOnly = true)
  public List<CohortSwapCandidate> findCohortSwapCandidates(
      Long scheduledClassId, int academicYear, int semester) {

    List<ScheduledClass> allClasses = scheduledClassRepository.findAllWithDetailsByPeriod(academicYear, semester);

    ScheduledClass target = allClasses.stream()
        .filter(sc -> sc.getId().equals(scheduledClassId))
        .findFirst()
        .orElseThrow(
            () -> new IllegalArgumentException("ScheduledClass not found: " + scheduledClassId));

    Long targetCohortId = target.getCohort().getId();
    Long targetSubjectId = target.getSubject().getId();

    TimetableSolution solution = solutionMapper.fromScheduledClasses(
        allClasses,
        timeslotRepository.findAll(),
        roomRepository.findAll(),
        academicYear,
        semester);

    LessonAssignment targetLesson = solution.getLessonAssignments().stream()
        .filter(la -> la.getId().equals(scheduledClassId))
        .findFirst()
        .orElseThrow();

    LessonAssignment optionalPair = findOptionalPair(targetLesson, solution.getLessonAssignments(), scheduledClassId);

    String excludedOptionalGroupId = targetLesson.getOptionalGroupId();

    List<LessonAssignment> sameCohortOthers = solution.getLessonAssignments().stream()
        .filter(la -> !la.getId().equals(scheduledClassId))
        .filter(la -> la.getCohortSubject().getCohort().getId().equals(targetCohortId))
        .filter(la -> !la.getCohortSubject().getSubject().getId().equals(targetSubjectId))
        .filter(
            la -> excludedOptionalGroupId == null
                || !excludedOptionalGroupId.equals(la.getOptionalGroupId()))
        .toList();

    TimeslotInfo originalTimeslotA = targetLesson.getTimeslot();
    final TimeslotInfo pairOriginalTimeslot = optionalPair != null ? optionalPair.getTimeslot() : null;

    List<CohortSwapCandidate> valid = new ArrayList<>();

    for (LessonAssignment candidate : sameCohortOthers) {
      TimeslotInfo originalTimeslotB = candidate.getTimeslot();
      RoomInfo originalRoomB = candidate.getRoom();

      // Candidate's own optional-group sibling — excluded ids: scheduledClassId
      // (target) and candidate's own id, so neither can match as its own pair.
      LessonAssignment candidatePair = findOptionalPair(
          candidate, solution.getLessonAssignments(), scheduledClassId, candidate.getId());
      final TimeslotInfo candidatePairOriginalTimeslot = candidatePair != null ? candidatePair.getTimeslot() : null;

      // Swap timeslots; rooms stay with their respective lessons
      targetLesson.setTimeslot(originalTimeslotB);
      candidate.setTimeslot(originalTimeslotA);
      if (optionalPair != null)
        optionalPair.setTimeslot(originalTimeslotB);
      if (candidatePair != null)
        candidatePair.setTimeslot(originalTimeslotA);

      try {
        solutionManager.update(solution);
        HardSoftScore score = solution.getScore();

        if (score != null && score.hardScore() == 0) {
          ScheduledClass candidateSc = allClasses.stream()
              .filter(sc -> sc.getId().equals(candidate.getId()))
              .findFirst()
              .orElseThrow();

          valid.add(
              new CohortSwapCandidate(
                  candidate.getId(),
                  candidateSc.getSubject().getName(),
                  originalTimeslotB.getDayOfWeek().toString(),
                  originalTimeslotB.getStartTime().toString(),
                  originalRoomB.getName()));
        }
      } finally {
        targetLesson.setTimeslot(originalTimeslotA);
        candidate.setTimeslot(originalTimeslotB);
        if (optionalPair != null)
          optionalPair.setTimeslot(pairOriginalTimeslot);
        if (candidatePair != null)
          candidatePair.setTimeslot(candidatePairOriginalTimeslot);
      }
    }

    log.info(
        "ScheduledClass {} → {}/{} valid cohort swaps",
        scheduledClassId,
        valid.size(),
        sameCohortOthers.size());
    return valid;
  }

  // ========================================
  // APPLY COHORT SWAP
  // ========================================

  @Transactional
  public void applyCohortSwap(Long scheduledClassIdA, Long scheduledClassIdB) {
    ScheduledClass scA = scheduledClassRepository
        .findById(scheduledClassIdA)
        .orElseThrow(() -> new IllegalArgumentException("Not found: " + scheduledClassIdA));
    ScheduledClass scB = scheduledClassRepository
        .findById(scheduledClassIdB)
        .orElseThrow(() -> new IllegalArgumentException("Not found: " + scheduledClassIdB));

    Timeslot timeslotA = scA.getTimeslot();
    Timeslot timeslotB = scB.getTimeslot();

    scA.setTimeslot(timeslotB);
    scB.setTimeslot(timeslotA);

    moveOptionalPairIfPresent(scA, scheduledClassIdA, scheduledClassIdB, timeslotA, timeslotB);
    moveOptionalPairIfPresent(scB, scheduledClassIdB, scheduledClassIdA, timeslotB, timeslotA);

    log.info(
        "Cohort swap: ScheduledClass {} ↔ ScheduledClass {}", scheduledClassIdA, scheduledClassIdB);
  }

  // ========================================
  // HELPERS
  // ========================================

  private boolean isCorrectPeriod(TimeslotInfo ts, int cohortYear) {
    boolean isOddYear = cohortYear % 2 != 0;
    return switch (ts.getPeriod()) {
      case MORNING -> isOddYear;
      case AFTERNOON -> !isOddYear;
      case EVENING -> false;
    };
  }

  /**
   * Finds {@code lesson}'s sibling in the same optional group and block
   * (i.e. its co-scheduled optional pair), if any, excluding the given ids
   * from the search. Operates purely on the in-memory solver representation
   * — no persistence, used for tentative scoring only.
   */
  private LessonAssignment findOptionalPair(
      LessonAssignment lesson, List<LessonAssignment> lessons, Long... excludedIds) {

    if (lesson.getOptionalGroupId() == null) {
      return null;
    }

    List<Long> excluded = Arrays.asList(excludedIds);

    return lessons.stream()
        .filter(la -> !excluded.contains(la.getId()))
        .filter(la -> lesson.getOptionalGroupId().equals(la.getOptionalGroupId()))
        .filter(la -> la.getBlockNumber() == lesson.getBlockNumber())
        .findFirst()
        .orElse(null);
  }

  /**
   * If the {@code ScheduledClass} belongs to an optional group, finds its sibling
   * in the same optional group that was originally assigned to
   * {@code originalTimeslot} and moves it to {@code newTimeslot}. The swap
   * participants are excluded from the search to prevent them from matching each
   * other.
   *
   * @param sc                 the scheduled class whose optional-group sibling
   *                           should be moved
   * @param scId               the ID of {@code sc}
   * @param otherParticipantId the ID of the other class participating in the
   *                           swap, excluded from the sibling lookup
   * @param originalTimeslot   the sibling's expected original timeslot
   * @param newTimeslot        the timeslot to assign to the sibling if found
   */
  private void moveOptionalPairIfPresent(
      ScheduledClass sc,
      Long scId,
      Long otherParticipantId,
      Timeslot originalTimeslot,
      Timeslot newTimeslot) {

    if (sc.getSubject().getOptionalGroup() == null) {
      return;
    }

    Long groupId = sc.getSubject().getOptionalGroup().getId();

    scheduledClassRepository
        .findAllWithDetailsByPeriod(
            sc.getTimetable().getAcademicYear(), sc.getTimetable().getSemester())
        .stream()
        .filter(candidate -> !candidate.getId().equals(scId))
        .filter(candidate -> !candidate.getId().equals(otherParticipantId))
        .filter(
            candidate -> candidate.getSubject().getOptionalGroup() != null
                && candidate.getSubject().getOptionalGroup().getId().equals(groupId))
        .filter(candidate -> candidate.getTimeslot().getId().equals(originalTimeslot.getId()))
        .findFirst()
        .ifPresent(
            pair -> {
              pair.setTimeslot(newTimeslot);
              scheduledClassRepository.save(pair);
              log.info(
                  "Optional pair ScheduledClass {} also moved to timeslot {} (cohort swap)",
                  pair.getId(),
                  newTimeslot.getId());
            });
  }

  // ========================================
  // RECORDS
  // ========================================

  public record CohortSwapCandidate(
      Long scheduledClassId,
      String subjectName,
      String dayOfWeek,
      String startTime,
      String roomName) {
  }

  public record ValidSlotResponse(
      long timeslotId,
      String dayOfWeek,
      String startTime,
      String endTime,
      boolean isSwap,
      Long swapWithId,
      String swapWithSubject,
      String swapWithCohort,
      String roomName,
      Long roomId) {

    static ValidSlotResponse empty(
        long timeslotId, String day, String start, String end, String roomName, Long roomId) {
      return new ValidSlotResponse(
          timeslotId, day, start, end, false, null, null, null, roomName, roomId);
    }

    static ValidSlotResponse swap(
        long timeslotId,
        String day,
        String start,
        String end,
        Long swapWithId,
        String subject,
        String cohort,
        String roomName,
        Long roomId) {
      return new ValidSlotResponse(
          timeslotId, day, start, end, true, swapWithId, subject, cohort, roomName, roomId);
    }
  }
}
