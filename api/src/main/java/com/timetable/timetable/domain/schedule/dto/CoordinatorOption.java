package com.timetable.timetable.domain.schedule.dto;

import com.timetable.timetable.domain.user.entity.ApplicationUser;

public record CoordinatorOption(Long id, String name) {
  public static CoordinatorOption from(ApplicationUser user) {
    return new CoordinatorOption(user.getId(), user.getUsername());
  }
}
