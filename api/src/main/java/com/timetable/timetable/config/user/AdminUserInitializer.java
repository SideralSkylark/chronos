package com.timetable.timetable.config.user;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.timetable.timetable.domain.user.entity.ApplicationUser;
import com.timetable.timetable.domain.user.entity.UserRole;
import com.timetable.timetable.domain.user.entity.UserRoleEntity;
import com.timetable.timetable.domain.user.repository.UserRepository;
import com.timetable.timetable.domain.user.repository.UserRoleRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Set;

@Component
@RequiredArgsConstructor
@Slf4j
public class AdminUserInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final UserRoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        String email = "admin@timetable.com";

        if (userRepository.findByEmail(email).isEmpty()) {
            log.info("Admin user not found — creating default admin...");

            UserRoleEntity adminRole = roleRepository.findByRole(UserRole.ADMIN)
                    .orElseGet(() -> {
                        UserRoleEntity newRole = new UserRoleEntity();
                        newRole.setRole(UserRole.ADMIN);
                        return roleRepository.save(newRole);
                    });

            UserRoleEntity userRole = roleRepository.findByRole(UserRole.USER)
                    .orElseGet(() -> {
                        UserRoleEntity newRole = new UserRoleEntity();
                        newRole.setRole(UserRole.USER);
                        return roleRepository.save(newRole);
                    });

            ApplicationUser admin = ApplicationUser.builder()
                    .username("admin")
                    .email(email)
                    .password(passwordEncoder.encode("admin123"))
                    .roles(Set.of(adminRole, userRole))
                    .build();

            admin.activate();
            userRepository.save(admin);

            log.info("Default admin created: {} / {}", admin.getEmail(), "admin123");
        } else {
            log.info("Admin user already exists — checking roles...");

            ApplicationUser admin = userRepository.findByEmail(email).get();

            UserRoleEntity userRole = roleRepository.findByRole(UserRole.USER)
                    .orElseGet(() -> {
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
