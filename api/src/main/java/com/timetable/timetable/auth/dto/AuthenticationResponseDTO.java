package com.timetable.timetable.auth.dto;

import java.time.LocalDateTime;
import java.util.Set;

/**
 * Data Transfer Object for authentication responses, providing user profile information.
 *
 * @param id        The unique identifier of the user.
 * @param username  The user's username.
 * @param email     The user's email address.
 * @param roles     The set of roles assigned to the user.
 * @param updatedAt The timestamp when the user's profile was last updated.
 */
public record AuthenticationResponseDTO(
	Long id,
	String username,
	String email,
	Set<String> roles,
	LocalDateTime updatedAt
){}

