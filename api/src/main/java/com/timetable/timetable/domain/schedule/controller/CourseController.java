package com.timetable.timetable.domain.schedule.controller;

import com.timetable.timetable.common.response.ApiResponse;
import com.timetable.timetable.common.response.ResponseFactory;
import com.timetable.timetable.domain.schedule.dto.CoordinatorOption;
import com.timetable.timetable.domain.schedule.dto.CourseListResponse;
import com.timetable.timetable.domain.schedule.dto.CourseResponse;
import com.timetable.timetable.domain.schedule.dto.CreateCourseRequest;
import com.timetable.timetable.domain.schedule.dto.UpdateCourseRequest;
import com.timetable.timetable.domain.schedule.service.CourseService;
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
@RequestMapping("api/v1/courses")
public class CourseController {
  private final CourseService courseService;

  @PostMapping
  public ResponseEntity<ApiResponse<CourseResponse>> create(
      @Valid @RequestBody CreateCourseRequest request) {
    return ResponseFactory.ok(courseService.createCourse(request), "Course created");
  }

  @GetMapping
  public ResponseEntity<ApiResponse<PagedModel<CourseListResponse>>> getAll(Pageable pageable) {
    return ResponseFactory.ok(
        new PagedModel<>(courseService.findAllWithSubjectCount(pageable)), "Courses fetched");
  }

  @GetMapping("/coordinators")
  public ResponseEntity<ApiResponse<PagedModel<CoordinatorOption>>> getCoordinators(
      Pageable pageable) {
    return ResponseFactory.ok(
        new PagedModel<>(courseService.getAvailableCoordinators(pageable)), "Coordinators fetched");
  }

  @GetMapping("/{id}")
  public ResponseEntity<ApiResponse<CourseResponse>> getById(@PathVariable Long id) {
    return ResponseFactory.ok(courseService.getById(id), "Course fetched");
  }

  @PutMapping("/{id}")
  public ResponseEntity<ApiResponse<CourseResponse>> update(
      @PathVariable Long id, @Valid @RequestBody UpdateCourseRequest request) {
    return ResponseFactory.ok(courseService.updateCourse(id, request), "Course updated");
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> delete(@PathVariable Long id) {
    courseService.deleteCourse(id);
    return ResponseEntity.noContent().build();
  }
}
