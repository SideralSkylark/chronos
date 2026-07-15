package com.timetable.timetable.config;

import com.timetable.timetable.domain.schedule.entity.Course;
import com.timetable.timetable.domain.schedule.entity.Subject;
import com.timetable.timetable.domain.schedule.repository.CourseRepository;
import com.timetable.timetable.domain.schedule.repository.SubjectRepository;
import com.timetable.timetable.domain.user.entity.AccountStatus;
import com.timetable.timetable.domain.user.entity.ApplicationUser;
import com.timetable.timetable.domain.user.entity.UserRole;
import com.timetable.timetable.domain.user.entity.UserRoleEntity;
import com.timetable.timetable.domain.user.repository.UserRepository;
import com.timetable.timetable.domain.user.repository.UserRoleRepository;
import jakarta.transaction.Transactional;
import java.time.DayOfWeek;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class BusinessSimulationInitializer implements CommandLineRunner {

  private final SubjectRepository subjectRepository;
  private final CourseRepository courseRepository;
  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final UserRoleRepository roleRepository;

  @Override
  @Transactional
  public void run(String... args) {
    ensureSimulationTeamUser();
    initSimulacao("Simulação Empresarial I", 3, 2);
    initSimulacao("Simulação Empresarial II", 4, 1);
  }

  private void ensureSimulationTeamUser() {
    userRepository
        .findByEmail("equipa@simulacao.com")
        .orElseGet(
            () -> {
              log.info("Creating simulation team user");
              UserRoleEntity userRole =
                  roleRepository
                      .findByRole(UserRole.USER)
                      .orElseGet(
                          () -> {
                            UserRoleEntity newRole = new UserRoleEntity();
                            newRole.setRole(UserRole.USER);
                            return roleRepository.save(newRole);
                          });
              UserRoleEntity teacherRole =
                  roleRepository
                      .findByRole(UserRole.TEACHER)
                      .orElseGet(
                          () -> {
                            UserRoleEntity newRole = new UserRoleEntity();
                            newRole.setRole(UserRole.TEACHER);
                            return roleRepository.save(newRole);
                          });

              return userRepository.save(
                  ApplicationUser.builder()
                      .username("A Equipa")
                      .email("equipa@simulacao.com")
                      .password(passwordEncoder.encode(UUID.randomUUID().toString()))
                      .roles(Set.of(userRole, teacherRole))
                      .status(AccountStatus.ACTIVE)
                      .simulationTeam(true)
                      .build());
            });
  }

  public void initSimulationForCourse(Course course) {
    ensureSimulationTeamUser();
    initSimulacaoForCourse("Simulação Empresarial I", 3, 2, course);
    initSimulacaoForCourse("Simulação Empresarial II", 4, 1, course);
  }

  private void initSimulacaoForCourse(
      String name, int targetYear, int targetSemester, Course course) {
    boolean exists =
        subjectRepository.existsByNameAndCourseAndTargetYearAndTargetSemester(
            name, course, targetYear, targetSemester);
    if (exists) {
      log.debug("Already exists: {} for {}", name, course.getName());
      return;
    }
    subjectRepository.save(
        Subject.builder()
            .name(name)
            .course(course)
            .targetYear(targetYear)
            .targetSemester(targetSemester)
            .credits(5)
            .fixedDaySession(true)
            .fixedDayOfWeek(DayOfWeek.WEDNESDAY)
            .build());
    log.info("Initialized: {} for course {}", name, course.getName());
  }

  private void initSimulacao(String name, int targetYear, int targetSemester) {
    List<Course> courses = courseRepository.findByHasBusinessSimulationTrue();

    if (courses.isEmpty()) {
      log.debug("No courses with hasBusinessSimulation=true, skipping {}", name);
      return;
    }

    for (Course course : courses) {
      boolean exists =
          subjectRepository.existsByNameAndCourseAndTargetYearAndTargetSemester(
              name, course, targetYear, targetSemester);

      if (exists) {
        log.debug("Already exists: {} for {}", name, course.getName());
        continue;
      }

      subjectRepository.save(
          Subject.builder()
              .name(name)
              .course(course)
              .targetYear(targetYear)
              .targetSemester(targetSemester)
              .credits(5)
              .fixedDaySession(true)
              .fixedDayOfWeek(DayOfWeek.WEDNESDAY)
              .build());

      log.info("Initialized: {} for course {}", name, course.getName());
    }
  }
}
