package com.timetable.timetable.config;

import com.timetable.timetable.domain.user.entity.UserRole;
import com.timetable.timetable.domain.user.entity.UserRoleEntity;
import com.timetable.timetable.domain.user.repository.UserRoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class RoleInitializer implements CommandLineRunner {

  private final UserRoleRepository roleRepository;

  @Override
  @Transactional
  public void run(String... args) {
    for (UserRole role : UserRole.values()) {
      roleRepository
          .findByRole(role)
          .orElseGet(
              () -> {
                UserRoleEntity newRole = new UserRoleEntity();
                newRole.setRole(role);
                return roleRepository.save(newRole);
              });
    }
  }
}
