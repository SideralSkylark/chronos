package com.timetable.timetable.domain.user.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import com.timetable.timetable.common.response.ApiResponse;
import com.timetable.timetable.common.response.ResponseFactory;
import com.timetable.timetable.domain.schedule.entity.TeacherType;
import com.timetable.timetable.domain.user.dto.UserFilterParams;
import com.timetable.timetable.domain.user.dto.UserResponse;
import com.timetable.timetable.domain.user.entity.AccountStatus;
import com.timetable.timetable.domain.user.entity.UserRole;
import com.timetable.timetable.domain.user.service.UserService;

import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedModel;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("api/v1/teachers")
@RequiredArgsConstructor
@Slf4j
public class TeacherController {
    private final UserService userService;

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
            "teachers fetched sucessfully"
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UserResponse>> getById(@PathVariable Long id) {
        return ResponseFactory.ok(
            userService.getUserByRoleAndId(UserRole.TEACHER, id),
            "teachers fetched sucessfully"
        );
    }
}
