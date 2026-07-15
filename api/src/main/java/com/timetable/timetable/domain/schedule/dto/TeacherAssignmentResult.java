package com.timetable.timetable.domain.schedule.dto;

import com.timetable.timetable.domain.schedule.entity.CohortSubject;
import com.timetable.timetable.domain.user.entity.ApplicationUser;

/** info pertaining to teacher that was assigned to a {@link CohortSubject} */
public record TeacherAssignmentResult(ApplicationUser teacher, boolean isPhantom, String warning) {}
