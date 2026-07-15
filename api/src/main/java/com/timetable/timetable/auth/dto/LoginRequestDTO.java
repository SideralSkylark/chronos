package com.timetable.timetable.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Data Transfer Object for login requests.
 *
 * @param email The user's email address (must be a valid email format).
 * @param password The user's password (must be between 8 and 100 characters).
 */
public record LoginRequestDTO(
    @NotBlank(message = "Email is required") @Email String email,
    @NotBlank(message = "Password is required")
        @Size(min = 8, max = 100, message = "Password must be between 8 and 100 characters")
        String password) {}
