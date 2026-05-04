package com.timetable.timetable.domain.user.controller;

import com.timetable.timetable.common.response.ResponseFactory;
import com.timetable.timetable.common.response.ApiResponse;
import com.timetable.timetable.domain.user.dto.UpdateUserProfileDTO;
import com.timetable.timetable.domain.user.dto.UserResponse;
import com.timetable.timetable.domain.user.entity.ApplicationUser;
import com.timetable.timetable.domain.user.exception.UserNotFoundException;
import com.timetable.timetable.domain.user.service.UserService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for managing actions related to the authenticated user.
 * <p>
 * This controller provides endpoints for:
 * <ul>
 *   <li>Retrieving the logged-in user's profile</li>
 *   <li>Updating user details</li>
 *   <li>Deleting user account</li>
 * </ul>
 *
 * <p>All endpoints require a valid authenticated session. Role-based
 * access control is enforced where applicable.</p>
 *
 * @author Sideral Skylark
 *
 * @since 2025-06-22
 */
@RestController
@RequestMapping("api/v1/users")
@RequiredArgsConstructor
@Slf4j
public class UserController {

    private final UserService userService;

    /**
     * Get the profile of the currently authenticated user.
     * <p>
     * Retrieves user details based on the authentication context.
     * Converts the internal {@link ApplicationUser} to {@link UserResponseDTO}.
     *
     * @return 200 OK with the user data if authenticated and found
     * @throws UserNotFoundException if the user cannot be found in the database
     * @throws IllegalStateException if the authentication context is invalid
     */
	@PreAuthorize("hasRole('USER')")
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> getUserDetails() {
		log.debug("Fetching authenticated user profile");
        return ResponseFactory.ok(
            userService.getAuthenticatedUserProfile(),
            "User profile retrieved successfully"
        );
    }

    /**
     * Update the authenticated user's profile information.
     * <p>
     * Updates basic fields like username, email, and enabled status.
     * Note that only authenticated users can update their own data.
     *
     * @param payload A {@link UpdateUserProfileDTO} containing validated updated fields
     * @return 200 OK with the updated user data
     * @throws UserNotFoundException if the user cannot be found
     * @throws IllegalStateException if the authentication context is invalid
     */
	@PreAuthorize("hasRole('USER')")
    @PutMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> updateUserDetails(
        @Valid @RequestBody UpdateUserProfileDTO payload) {
        log.debug("Updating authenticated user profile");
		return ResponseFactory.ok(
            userService.updateAuthenticatedUserProfile(payload),
			"User profile updated successfully"
        );
    }

    /**
     * Permanently delete the currently authenticated user's account.
     * <p>
     * This operation is irreversible and requires valid authentication.
     *
     * @return 204 No Content if deletion was successful
     * @throws UserNotFoundException if the user cannot be found
     * @throws IllegalStateException if the authentication context is invalid
     */
	@PreAuthorize("hasRole('USER')")
    @DeleteMapping("/me")
    public ResponseEntity<Void> deleteUser() {
		log.debug("Deleting authenticated user account");
        userService.deleteAuthenticatedUserProfile();
        return ResponseEntity.noContent().build();
    }
}
