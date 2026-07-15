package com.timetable.timetable.config.user;

import com.timetable.timetable.domain.user.entity.ApplicationUser;
import com.timetable.timetable.domain.user.entity.UserRole;
import com.timetable.timetable.domain.user.entity.UserRoleEntity;
import com.timetable.timetable.domain.user.repository.UserRepository;
import com.timetable.timetable.domain.user.repository.UserRoleRepository;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class AdminUserInitializer implements CommandLineRunner {

  private final UserRepository userRepository;
  private final UserRoleRepository roleRepository;
  private final PasswordEncoder passwordEncoder;

  @Value("${app.admin.password}")
  private String adminPassword;

  @Override
  public void run(String... args) {
    String email = "admin@timetable.com";

    if (userRepository.findByEmail(email).isEmpty()) {
      log.info("Admin user not found — creating default admin...");

      UserRoleEntity adminRole =
          roleRepository
              .findByRole(UserRole.ADMIN)
              .orElseGet(
                  () -> {
                    UserRoleEntity newRole = new UserRoleEntity();
                    newRole.setRole(UserRole.ADMIN);
                    return roleRepository.save(newRole);
                  });

      UserRoleEntity userRole =
          roleRepository
              .findByRole(UserRole.USER)
              .orElseGet(
                  () -> {
                    UserRoleEntity newRole = new UserRoleEntity();
                    newRole.setRole(UserRole.USER);
                    return roleRepository.save(newRole);
                  });

      ApplicationUser admin =
          ApplicationUser.builder()
              .username("admin")
              .email(email)
              .password(passwordEncoder.encode(adminPassword))
              .roles(Set.of(adminRole, userRole))
              .build();

      admin.activate();
      userRepository.save(admin);

      log.info("Default admin created: {}", admin.getEmail());
    } else {
      log.info("Admin user already exists — checking roles...");

      ApplicationUser admin = userRepository.findByEmail(email).get();

      UserRoleEntity userRole =
          roleRepository
              .findByRole(UserRole.USER)
              .orElseGet(
                  () -> {
                    UserRoleEntity newRole = new UserRoleEntity();
                    newRole.setRole(UserRole.USER);
                    return roleRepository.save(newRole);
                  });

      if (!admin.getRoles().contains(userRole)) {
        admin.getRoles().add(userRole);
        userRepository.save(admin);
        log.info("Added missing USER role to existing admin.");
      }
    }
  }
}
