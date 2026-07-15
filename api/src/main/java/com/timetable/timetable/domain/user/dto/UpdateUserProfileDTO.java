package com.timetable.timetable.domain.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** DTO for user self-profile updates. */
public record UpdateUserProfileDTO(
    @NotBlank(message = "Username must not be blank")
        @Size(min = 3, max = 50, message = "Username must be between 3 and 50 catacters")
        String username,
    @NotBlank(message = "Email must not be blank") @Email(message = "Email should be valid")
        String email) {}
