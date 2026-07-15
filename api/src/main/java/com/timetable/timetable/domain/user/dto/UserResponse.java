package com.timetable.timetable.domain.user.dto;

import com.timetable.timetable.domain.schedule.entity.TeacherType;
import java.util.Set;

/** DTO representing a user's public information. */
public record UserResponse(
    Long id,
    String username,
    String email,
    Set<String> roles,
    boolean enabled,
    TeacherType teacherType) {}
