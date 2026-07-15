package com.timetable.timetable.domain.user.dto;

import com.timetable.timetable.domain.schedule.entity.TeacherType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

/** DTO for creating a new user. */
public record CreateUser(
    @NotBlank(message = "Username is required") String username,
    @Email @NotBlank(message = "Email is required") String email,
    @NotBlank(message = "Password is required")
        @Size(min = 8, max = 100, message = "Password must be between 8 and 100 characters")
        String password,
    @Size(min = 1, max = 3) List<@NotBlank(message = "Role must not be blank") String> roles,
    TeacherType teacherType) {}
