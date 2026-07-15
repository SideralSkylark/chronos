package com.timetable.timetable.domain.dashboard.controller;

import com.timetable.timetable.common.response.ApiResponse;
import com.timetable.timetable.common.response.ResponseFactory;
import com.timetable.timetable.domain.dashboard.dto.DashboardStatsDTO;
import com.timetable.timetable.domain.dashboard.service.DashboardStatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
public class DashboardStatsController {

  private final DashboardStatsService dashboardStatsService;

  @GetMapping("/stats")
  @PreAuthorize("hasAnyRole('ADMIN', 'DIRECTOR', 'ASISTENT')")
  @Transactional(readOnly = true)
  public ResponseEntity<ApiResponse<DashboardStatsDTO>> getStats(
      @RequestParam(required = false) Integer academicYear,
      @RequestParam(required = false) Integer semester) {
    return ResponseFactory.ok(dashboardStatsService.computeStats(academicYear, semester));
  }
}
