package com.timetable.timetable.domain.schedule.controller;

import com.timetable.timetable.common.response.ApiResponse;
import com.timetable.timetable.common.response.ResponseFactory;
import com.timetable.timetable.domain.schedule.dto.CreateScheduledClassRequest;
import com.timetable.timetable.domain.schedule.dto.ScheduledClassResponse;
import com.timetable.timetable.domain.schedule.dto.UpdateScheduledClassRequest;
import com.timetable.timetable.domain.schedule.service.ScheduledClassService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedModel;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("api/v1/timeslots")
public class ScheduledClassController {
  private final ScheduledClassService scheduledClassService;

  @PostMapping
  public ResponseEntity<ApiResponse<ScheduledClassResponse>> create(
      @Valid @RequestBody CreateScheduledClassRequest request) {
    return ResponseFactory.ok(
        scheduledClassService.createScheduledClass(request), "Time slot created successfully.");
  }

  @GetMapping
  public ResponseEntity<ApiResponse<PagedModel<ScheduledClassResponse>>> getAll(Pageable pageable) {
    return ResponseFactory.ok(
        new PagedModel<>(scheduledClassService.getAll(pageable)),
        "Time slots fetched successfully.");
  }

  @GetMapping("/{id}")
  public ResponseEntity<ApiResponse<ScheduledClassResponse>> getById(@PathVariable Long id) {
    return ResponseFactory.ok(scheduledClassService.getById(id), "Time slot fetched successfully.");
  }

  @PutMapping("/{id}")
  public ResponseEntity<ApiResponse<ScheduledClassResponse>> update(
      @PathVariable Long id, @Valid @RequestBody UpdateScheduledClassRequest request) {
    return ResponseFactory.ok(
        scheduledClassService.updateScheduledClass(id, request), "Time slot updated successfully.");
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> delete(@PathVariable Long id) {
    scheduledClassService.deleteScheduledClass(id);
    return ResponseEntity.noContent().build();
  }
}
