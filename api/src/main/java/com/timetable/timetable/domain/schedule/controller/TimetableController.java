package com.timetable.timetable.domain.schedule.controller;

import com.timetable.timetable.common.response.ApiResponse;
import com.timetable.timetable.common.response.ResponseFactory;
import com.timetable.timetable.domain.schedule.dto.CreateTimetableRequest;
import com.timetable.timetable.domain.schedule.dto.TimetableResponse;
import com.timetable.timetable.domain.schedule.dto.UpdateTimetableRequest;
import com.timetable.timetable.domain.schedule.service.TimetableService;

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

import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("api/v1/timetables")
public class TimetableController {
    private final TimetableService timetableService;

    @PostMapping
    public ResponseEntity<ApiResponse<TimetableResponse>> create(@Valid @RequestBody CreateTimetableRequest request) {
        return ResponseFactory.ok(
                TimetableResponse.from(timetableService.createTimetable(request)),
                "Timetable created successfully.");
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PagedModel<TimetableResponse>>> getAll(Pageable pageable) {
        return ResponseFactory.ok(
                new PagedModel<>(timetableService.getAll(pageable).map(TimetableResponse::from)),
                "Timetables fetched successfully.");
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<TimetableResponse>> getById(@PathVariable Long id) {
        return ResponseFactory.ok(
                TimetableResponse.from(timetableService.getById(id)),
                "Timetable fetched successfully.");
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<TimetableResponse>> update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateTimetableRequest request) {
        return ResponseFactory.ok(
                TimetableResponse.from(timetableService.updateTimetable(id, request)),
                "Timetable updated successfully.");
    }

    @PostMapping("/{id}/submit")
    @Transactional
    public ResponseEntity<ApiResponse<TimetableResponse>> submit(@PathVariable Long id) {
        return ResponseFactory.ok(
                TimetableResponse.from(timetableService.submitForApproval(id)),
                "Timetable submitted for approval.");
    }

    @PostMapping("/{id}/approve")
    @Transactional
    public ResponseEntity<ApiResponse<TimetableResponse>> approve(@PathVariable Long id) {
        return ResponseFactory.ok(
                TimetableResponse.from(timetableService.approve(id)),
                "Timetable approved.");
    }

    @PostMapping("/{id}/reject")
    @Transactional
    public ResponseEntity<ApiResponse<TimetableResponse>> reject(@PathVariable Long id) {
        return ResponseFactory.ok(
                TimetableResponse.from(timetableService.reject(id)),
                "Timetable rejected and reverted to draft.");
    }

    @PostMapping("/{id}/publish")
    @Transactional
    public ResponseEntity<ApiResponse<TimetableResponse>> publish(@PathVariable Long id) {
        return ResponseFactory.ok(
                TimetableResponse.from(timetableService.publishTimetable(id)),
                "Timetable published.");
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        timetableService.deleteTimetable(id);
        return ResponseEntity.noContent().build();
    }
}
