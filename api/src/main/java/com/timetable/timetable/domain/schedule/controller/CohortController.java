package com.timetable.timetable.domain.schedule.controller;

import com.timetable.timetable.common.response.ApiResponse;
import com.timetable.timetable.common.response.ResponseFactory;
import com.timetable.timetable.domain.schedule.dto.CohortFilterParams;
import com.timetable.timetable.domain.schedule.dto.CohortListResponse;
import com.timetable.timetable.domain.schedule.dto.CohortResponse;
import com.timetable.timetable.domain.schedule.dto.CohortSummaryResponse;
import com.timetable.timetable.domain.schedule.dto.ConfirmCohortRequest;
import com.timetable.timetable.domain.schedule.dto.CreateCohortRequest;
import com.timetable.timetable.domain.schedule.dto.UpdateCohortRequest;
import com.timetable.timetable.domain.schedule.dto.UpdateCohortStudentsRequest;
import com.timetable.timetable.domain.schedule.entity.CohortStatus;
import com.timetable.timetable.domain.schedule.service.CohortService;

import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedModel;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("api/v1/cohorts")
public class CohortController {
    private final CohortService cohortService;

    @PostMapping
    public ResponseEntity<ApiResponse<CohortResponse>> create(@Valid @RequestBody CreateCohortRequest request) {
        return ResponseFactory.ok(
                cohortService.createCohortResponse(request),
                "Cohort created successfully.");
    }

    @PatchMapping("/{id}/confirm")
    public ResponseEntity<ApiResponse<CohortResponse>> confirm(
            @PathVariable Long id,
            @Valid @RequestBody ConfirmCohortRequest request) {
        return ResponseFactory.ok(
                cohortService.confirmCohort(id, request.studentCount()),
                "Ingressos confirmados com sucesso.");
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PagedModel<CohortListResponse>>> getAll(
            Pageable pageable,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Long courseId,
            @RequestParam(required = false) Integer academicYear,
            @RequestParam(required = false) Integer semester,
            @RequestParam(required = false) CohortStatus status) {

        CohortFilterParams filters = new CohortFilterParams();
        filters.setName(name);
        filters.setCourseId(courseId);
        filters.setAcademicYear(academicYear);
        filters.setSemester(semester);
        filters.setStatus(status);

        return ResponseFactory.ok(
                new PagedModel<>(cohortService.findAll(pageable, filters)),
                "Cohorts fetched successfully.");
    }

    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<CohortSummaryResponse>> getSummary(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Long courseId,
            @RequestParam(required = false) Integer academicYear,
            @RequestParam(required = false) Integer semester) {

        CohortFilterParams filters = new CohortFilterParams();
        filters.setName(name);
        filters.setCourseId(courseId);
        filters.setAcademicYear(academicYear);
        filters.setSemester(semester);

        return ResponseFactory.ok(
                cohortService.getSummary(filters),
                "Summary fetched successfully.");
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CohortResponse>> getById(@PathVariable Long id) {
        return ResponseFactory.ok(
                cohortService.getResponseById(id),
                "Cohort fetched successfully.");
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<CohortResponse>> update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateCohortRequest request) {
        return ResponseFactory.ok(
                cohortService.updateCohort(id, request),
                "Cohort updated successfully.");
    }

    @PutMapping("/{id}/students")
    public ResponseEntity<ApiResponse<CohortResponse>> updateStudents(
            @PathVariable Long id,
            @Valid @RequestBody UpdateCohortStudentsRequest request) {
        return ResponseFactory.ok(
                cohortService.updateStudents(id, request.studentIds()),
                "Students updated successfully.");
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        cohortService.deleteCohort(id);
        return ResponseEntity.noContent().build();
    }
}
