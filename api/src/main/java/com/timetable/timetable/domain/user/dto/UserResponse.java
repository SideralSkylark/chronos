package com.timetable.timetable.domain.user.dto;

import java.util.Set;

import com.timetable.timetable.domain.schedule.entity.TeacherType;

/**
 * DTO representing a user's public information.
 */
public record UserResponse(
	Long id,
    String username,
    String email,
    Set<String> roles,
    boolean enabled,
    TeacherType teacherType
) { }
