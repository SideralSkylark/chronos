package com.timetable.timetable.domain.schedule.dto;

import com.timetable.timetable.domain.schedule.entity.OptionalGroup;
import com.timetable.timetable.domain.schedule.entity.Subject;
import java.util.Set;
import java.util.stream.Collectors;

public record OptionalGroupResponse(Long id, String name, Set<Long> subjects) {

  public static OptionalGroupResponse from(OptionalGroup group) {
    return new OptionalGroupResponse(
        group.getId(),
        group.getName(),
        group.getSubjects().stream().map(Subject::getId).collect(Collectors.toSet()));
  }
}
