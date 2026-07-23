package com.timetable.timetable.scheduler_engine.solver;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/permutations")
@RequiredArgsConstructor
public class PermutationController {

  private final PermutationService permutationService;

  @PostMapping("/valid-slots")
  public ResponseEntity<List<PermutationService.ValidSlotResponse>> getValidSlots(
      @RequestBody ValidSlotsRequest request) {
    return ResponseEntity.ok(
        permutationService.findValidSlots(
            request.scheduledClassId(), request.academicYear(), request.semester()));
  }

  /**
   * Body: { "scheduledClassId": 42, "targetTimeslotId": 7, "swapWithId": 55 } swapWithId is
   * optional — null means move to empty slot.
   */
  @PostMapping("/apply")
  public ResponseEntity<Void> applySwap(@RequestBody ApplySwapRequest request) {
    permutationService.applySwap(
        request.scheduledClassId(),
        request.targetTimeslotId(),
        request.targetRoomId(),
        request.swapWithId() != null ? List.of(request.swapWithId()) : null);
    return ResponseEntity.noContent().build();
  }

  @PostMapping("/cohort-swap/candidates")
  public ResponseEntity<List<PermutationService.CohortSwapCandidate>> getCohortSwapCandidates(
      @RequestBody CohortSwapCandidatesRequest request) {
    return ResponseEntity.ok(
        permutationService.findCohortSwapCandidates(
            request.scheduledClassId(), request.academicYear(), request.semester()));
  }

  @PostMapping("/cohort-swap/apply")
  public ResponseEntity<Void> applyCohortSwap(@RequestBody ApplyCohortSwapRequest request) {
    permutationService.applyCohortSwap(request.scheduledClassIdA(), request.scheduledClassIdB());
    return ResponseEntity.noContent().build();
  }

  record CohortSwapCandidatesRequest(Long scheduledClassId, int academicYear, int semester) {}

  record ApplyCohortSwapRequest(Long scheduledClassIdA, Long scheduledClassIdB) {}

  record ValidSlotsRequest(Long scheduledClassId, int academicYear, int semester) {}

  record ApplySwapRequest(
      Long scheduledClassId, Long targetTimeslotId, Long targetRoomId, Long swapWithId) {}
}
