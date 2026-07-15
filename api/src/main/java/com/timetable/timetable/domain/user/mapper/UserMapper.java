package com.timetable.timetable.domain.user.mapper;

import com.timetable.timetable.domain.user.dto.UserResponse;
import com.timetable.timetable.domain.user.entity.ApplicationUser;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

  public UserResponse toDTO(ApplicationUser user) {
    if (user == null) return null;

    Set<String> roles =
        user.getRoles() == null
            ? Collections.emptySet()
            : user.getRoles().stream().map(r -> r.getRole().name()).collect(Collectors.toSet());

    return new UserResponse(
        user.getId(),
        user.getUsername(),
        user.getEmail(),
        roles,
        user.isEnabled(),
        user.getTeacherType());
  }
}
