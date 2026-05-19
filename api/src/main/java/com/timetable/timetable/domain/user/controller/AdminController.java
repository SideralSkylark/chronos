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

/**
 * REST controller for administrative user management.
 *
 * <p>Provides endpoints for creating, updating, deleting, and listing users.
 * Access is restricted to users with ADMIN, ASISTENT, or DIRECTOR roles.</p>
 *
 * @author Sideral Skylark
 */
@RestController
@RequestMapping("api/v1/admins")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasAnyRole('ADMIN', 'ASISTENT', 'DIRECTOR')")
public class AdminController {
    private final UserService userService;

    /**
     * Creates a new user.
     *
     * @param createUser the user creation data
     * @return 200 OK with the created user
     */
    @PostMapping
    public ResponseEntity<ApiResponse<UserResponse>> createUser(
            @Valid @RequestBody CreateUser createUser) {
        return ResponseFactory.ok(
                userService.createUserResponse(createUser),
                "User created sucessfully.");
    }

    /**
     * Retrieves a paginated list of all users with optional filtering.
     *
     * @param pageable pagination details
     * @param username optional username filter
     * @param email optional email filter
     * @param role optional role filter
     * @param status optional account status filter
     * @param teacherType optional teacher type filter
     * @return 200 OK with a page of users
     */
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

    /**
     * Retrieves a user by their ID.
     *
     * @param id the user ID
     * @return 200 OK with the user data
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UserResponse>> getUserById(@PathVariable Long id) {
        return ResponseFactory.ok(
                userService.getUserById(id),
                "User fetched sucessfully.");
    }

    /**
     * Retrieves a paginated list of students.
     *
     * @param pageable pagination details
     * @param username optional username filter
     * @param email optional email filter
     * @param status optional account status filter
     * @return 200 OK with a page of students
     */
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

    /**
     * Updates a user's information.
     *
     * @param id the user ID
     * @param updateRequest the update data
     * @return 200 OK with the updated user
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<UserResponse>> updateUser(
            @PathVariable Long id,
            @RequestBody AdminUpdateUserDTO updateRequest) {
        return ResponseFactory.ok(
                userService.updateUserById(id, updateRequest),
                "User updated sucessfully.");
    }

    /**
     * Resets a user's password.
     *
     * @param id the user ID
     * @return 200 OK with the new password
     */
    @PostMapping("/{id}/reset-password")
    public ResponseEntity<ApiResponse<ResetPasswordResponse>> resetPassword(@PathVariable Long id) {
        return ResponseFactory.ok(
                userService.resetPassword(id),
                "User password reset successfully.");
    }

    /**
     * Deletes a user account.
     *
     * @param id the user ID
     * @return 204 No Content
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        userService.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
