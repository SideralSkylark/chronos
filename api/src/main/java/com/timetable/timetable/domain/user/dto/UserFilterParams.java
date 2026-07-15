package com.timetable.timetable.domain.user.dto;

import com.timetable.timetable.domain.schedule.entity.TeacherType;
import com.timetable.timetable.domain.user.entity.AccountStatus;
import com.timetable.timetable.domain.user.entity.UserRole;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Parameters for filtering user searches. */
@Data
@NoArgsConstructor
public class UserFilterParams {
  private String username;
  private String email;
  private UserRole role;
  private AccountStatus status;
  private TeacherType teacherType;
}
