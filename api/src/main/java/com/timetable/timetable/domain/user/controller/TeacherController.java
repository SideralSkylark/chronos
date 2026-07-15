package com.timetable.timetable.domain.user.controller;

import com.timetable.timetable.common.response.ApiResponse;
import com.timetable.timetable.common.response.ResponseFactory;
import com.timetable.timetable.domain.schedule.entity.TeacherType;
import com.timetable.timetable.domain.user.dto.UserFilterParams;
import com.timetable.timetable.domain.user.dto.UserResponse;
import com.timetable.timetable.domain.user.entity.AccountStatus;
import com.timetable.timetable.domain.user.entity.UserRole;
import com.timetable.timetable.domain.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedModel;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for teacher-related operations.
 *
 * <p>Provides endpoints for listing teachers and retrieving specific teacher details.
 *
 * @author Sideral Skylark
 */
@RestController
@RequestMapping("api/v1/teachers")
@RequiredArgsConstructor
@Slf4j
public class TeacherController {
  private final UserService userService;

  /**
   * Retrieves a paginated list of all teachers with optional filtering.
   *
   * @param pageable pagination details
   * @param username optional username filter
   * @param email optional email filter
   * @param status optional account status filter
   * @param teacherType optional teacher type filter
   * @return 200 OK with a page of teachers
   */
  @GetMapping
  public ResponseEntity<ApiResponse<PagedModel<UserResponse>>> getAllTeachers(
      Pageable pageable,
      @RequestParam(required = false) String username,
      @RequestParam(required = false) String email,
      @RequestParam(required = false) AccountStatus status,
      @RequestParam(required = false) TeacherType teacherType) {
    UserFilterParams filter = new UserFilterParams();
    filter.setUsername(username);
    filter.setEmail(email);
    filter.setStatus(status);
    filter.setTeacherType(teacherType);
    return ResponseFactory.ok(
        new PagedModel<>(userService.getUsersByRole(UserRole.TEACHER, pageable, filter)),
        "teachers fetched sucessfully");
  }

  /**
   * Retrieves a teacher by their ID.
   *
   * @param id the teacher's user ID
   * @return 200 OK with the teacher data
   */
  @GetMapping("/{id}")
  public ResponseEntity<ApiResponse<UserResponse>> getById(@PathVariable Long id) {
    return ResponseFactory.ok(
        userService.getUserByRoleAndId(UserRole.TEACHER, id), "teachers fetched sucessfully");
  }
}
