package com.timetable.timetable.domain.user.specification;

import com.timetable.timetable.domain.schedule.entity.TeacherType;
import com.timetable.timetable.domain.user.dto.UserFilterParams;
import com.timetable.timetable.domain.user.entity.AccountStatus;
import com.timetable.timetable.domain.user.entity.ApplicationUser;
import com.timetable.timetable.domain.user.entity.UserRole;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import org.springframework.data.jpa.domain.Specification;

public class UserSpecifications {

  public static Specification<ApplicationUser> withFilters(UserFilterParams f) {
    return Specification.allOf(
        usernameLike(f.getUsername()),
        emailLike(f.getEmail()),
        hasRole(f.getRole()),
        hasStatus(f.getStatus()),
        hasTeacherType(f.getTeacherType()));
  }

  public static Specification<ApplicationUser> hasRole(UserRole role) {
    return (root, query, cb) -> {
      if (role == null) return null;
      query.distinct(true);
      Join<ApplicationUser, ?> roles = root.join("roles", JoinType.INNER);
      return cb.equal(roles.get("role"), role);
    };
  }

  private static Specification<ApplicationUser> usernameLike(String username) {
    return (root, query, cb) ->
        username == null
            ? null
            : cb.like(cb.lower(root.get("username")), "%" + username.toLowerCase() + "%");
  }

  private static Specification<ApplicationUser> emailLike(String email) {
    return (root, query, cb) ->
        email == null
            ? null
            : cb.like(cb.lower(root.get("email")), "%" + email.toLowerCase() + "%");
  }

  private static Specification<ApplicationUser> hasStatus(AccountStatus status) {
    return (root, query, cb) -> status == null ? null : cb.equal(root.get("status"), status);
  }

  private static Specification<ApplicationUser> hasTeacherType(TeacherType type) {
    return (root, query, cb) -> type == null ? null : cb.equal(root.get("teacherType"), type);
  }
}
