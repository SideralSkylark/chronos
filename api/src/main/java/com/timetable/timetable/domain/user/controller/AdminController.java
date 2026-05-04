package com.timetable.timetable.domain.user.controller;

import com.timetable.timetable.common.response.ApiResponse;
import com.timetable.timetable.common.response.ResponseFactory;
import com.timetable.timetable.domain.schedule.entity.TeacherType;
import com.timetable.timetable.domain.user.dto.AdminUpdateUserDTO;
import com.timetable.timetable.domain.user.dto.CreateUser;
import com.timetable.timetable.domain.user.dto.ResetPasswordResponse;
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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

@RestController
@RequestMapping("api/v1/admins")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasAnyRole('ADMIN', 'ASISTENT', 'DIRECTOR')")
public class AdminController {
    private final UserService userService;

    @PostMapping
    public ResponseEntity<ApiResponse<UserResponse>> createUser(
            @Valid @RequestBody CreateUser createUser) {
        return ResponseFactory.ok(
                userService.createUserResponse(createUser),
                "User created sucessfully.");
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PagedModel<UserResponse>>> getUsers(Pageable pageable,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) UserRole role,
            @RequestParam(required = false) AccountStatus status,
            @RequestParam(required = false) TeacherType teacherType) {

        UserFilterParams filter = new UserFilterParams();
        filter.setUsername(username);
        filter.setEmail(email);
        filter.setRole(role);
        filter.setStatus(status);
        filter.setTeacherType(teacherType);
        log.info("params: {}", filter.toString());

        return ResponseFactory.ok(
                new PagedModel<>(userService.getAllUsers(pageable, filter)),
                "Users fetched sucessfully.");
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UserResponse>> getUserById(@PathVariable Long id) {
        return ResponseFactory.ok(
                userService.getUserById(id),
                "User fetched sucessfully.");
    }

    @GetMapping("/students")
    public ResponseEntity<ApiResponse<PagedModel<UserResponse>>> getStudents(
            Pageable pageable,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) AccountStatus status) {
        UserFilterParams filter = new UserFilterParams();
        filter.setUsername(username);
        filter.setEmail(email);
        filter.setStatus(status);
        return ResponseFactory.ok(
                new PagedModel<>(userService.getUsersByRole(UserRole.STUDENT, pageable, filter)),
                "Students fetched successfully.");
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<UserResponse>> updateUser(
            @PathVariable Long id,
            @RequestBody AdminUpdateUserDTO updateRequest) {
        return ResponseFactory.ok(
                userService.updateUserById(id, updateRequest),
                "User updated sucessfully.");
    }

    @PostMapping("/{id}/reset-password")
    public ResponseEntity<ApiResponse<ResetPasswordResponse>> resetPassword(@PathVariable Long id) {
        return ResponseFactory.ok(
                userService.resetPassword(id),
                "User password reset successfully.");
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        userService.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
