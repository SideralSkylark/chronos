package com.timetable.timetable.scheduler_engine.preparation;

import com.timetable.timetable.domain.schedule.dto.PhantomTeacherPolicy;
import com.timetable.timetable.domain.schedule.dto.TeacherAssignmentResult;
import com.timetable.timetable.domain.schedule.entity.*;
import com.timetable.timetable.domain.user.dto.CreateUser;
import com.timetable.timetable.domain.user.entity.ApplicationUser;
import com.timetable.timetable.domain.user.service.UserService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TeacherAssignmentService {

    private static final int MAX_PHANTOM_SLOTS = 50;

    private final UserService userService;

    /**
     * Assigns the least-loaded eligible teacher to a subject.
     * Falls back to a phantom teacher if all eligible teachers would exceed
     * their contract limits.
     */
    public TeacherAssignmentResult assignTeacher(
            Subject subject,
            List<CohortSubject> existingInSemester,
            PhantomTeacherPolicy policy) {

        int hoursNeeded = AcademicPolicy.calculateWeeklyHours(subject);
        Set<ApplicationUser> eligible = subject.getEligibleTeachers();

        if (eligible == null || eligible.isEmpty()) {
            return assignPhantom(subject, hoursNeeded, existingInSemester);
        }

        return findBestEligibleTeacher(subject, eligible, existingInSemester, hoursNeeded)
                .orElseGet(() -> {
                    log.warn("All eligible teachers are at capacity for subject: {}", subject.getName());
                    return assignPhantom(subject, hoursNeeded, existingInSemester);
                });
    }

    private Optional<TeacherAssignmentResult> findBestEligibleTeacher(
            Subject subject,
            Set<ApplicationUser> eligible,
            List<CohortSubject> existingInSemester,
            int hoursNeeded) {

        Map<Long, Integer> workload = computeWorkload(eligible, existingInSemester);

        return eligible.stream()
                .filter(t -> fitsWithinLimit(t, workload, hoursNeeded))
                .min(Comparator.comparingInt(t -> workload.getOrDefault(t.getId(), 0)))
                .map(teacher -> {
                    int before = workload.getOrDefault(teacher.getId(), 0);
                    log.info("Assigned {} to {} ({}h/week → {}h/week)",
                            teacher.getUsername(), subject.getName(), before, before + hoursNeeded);
                    return new TeacherAssignmentResult(teacher, false, null);
                });
    }

    /**
     * Finds or creates a phantom teacher with enough capacity for {@code hoursNeeded}.
     * Phantom slots are indexed (PHANTOM_{subjectId}, PHANTOM_{subjectId}_2, ...).
     */
    private TeacherAssignmentResult assignPhantom(
            Subject subject,
            int hoursNeeded,
            List<CohortSubject> existingInSemester) {

        String baseUsername = "PHANTOM_" + subject.getId();

        for (int slot = 1; slot <= MAX_PHANTOM_SLOTS; slot++) {
            String username = slot == 1 ? baseUsername : baseUsername + "_" + slot;
            ApplicationUser candidate = findOrCreatePhantom(username);
            int currentLoad = phantomLoad(username, existingInSemester);
            int limit = AcademicPolicy.getWeeklyHoursLimit(candidate);

            if (currentLoad + hoursNeeded <= limit) {
                log.warn("Using phantom teacher {} for subject {} ({}h + {}h)",
                        username, subject.getName(), currentLoad, hoursNeeded);
                return new TeacherAssignmentResult(candidate, true,
                        "Phantom teacher used for: " + subject.getName());
            }
        }

        throw new IllegalStateException(
                "Subject '%s' requires %dh/week but no phantom slot is available within institutional limits."
                        .formatted(subject.getName(), hoursNeeded));
    }

    private ApplicationUser findOrCreatePhantom(String username) {
        return userService.findByUsername(username)
                .orElseGet(() -> createPhantom(username));
    }

    private ApplicationUser createPhantom(String username) {
        CreateUser request = new CreateUser(
                username,
                "phantom+" + UUID.randomUUID() + "@placeholder.temp",
                "phantom123",
                List.of("user", "teacher"),
                TeacherType.PART_TIME);
        log.warn("Created phantom teacher {}", username);
        return userService.createUser(request);
    }

    /**
     * Computes weekly hours per teacher ID, restricted to the given eligible set.
     */
    private Map<Long, Integer> computeWorkload(
            Set<ApplicationUser> eligible,
            List<CohortSubject> existingInSemester) {

        Set<Long> eligibleIds = eligible.stream()
                .map(ApplicationUser::getId)
                .collect(Collectors.toSet());

        Map<Long, Integer> workload = new HashMap<>();

        for (CohortSubject cs : existingInSemester) {
            ApplicationUser teacher = cs.getAssignedTeacher();
            if (teacher != null && eligibleIds.contains(teacher.getId())) {
                workload.merge(teacher.getId(), cs.getWeeklyHours(), Integer::sum);
                log.trace("Teacher {} load +{}h from {}", teacher.getUsername(),
                        cs.getWeeklyHours(), cs.getDisplayName());
            }
        }

        return workload;
    }

    private boolean fitsWithinLimit(ApplicationUser teacher, Map<Long, Integer> workload, int hoursNeeded) {
        int limit = AcademicPolicy.getWeeklyHoursLimit(teacher);
        return workload.getOrDefault(teacher.getId(), 0) + hoursNeeded <= limit;
    }

    /**
     * Computes load for a phantom teacher by username across all existing assignments.
     */
    private int phantomLoad(String username, List<CohortSubject> existingInSemester) {
        return existingInSemester.stream()
                .filter(cs -> cs.getAssignedTeacher() != null
                        && cs.getAssignedTeacher().getUsername().equals(username))
                .mapToInt(CohortSubject::getWeeklyHours)
                .sum();
    }
}
