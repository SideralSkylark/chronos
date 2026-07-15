package com.timetable.timetable.domain.schedule.controller;

import com.timetable.timetable.domain.schedule.entity.CohortSubject;
import com.timetable.timetable.domain.schedule.entity.ScheduledClass;
import com.timetable.timetable.domain.schedule.entity.Timetable;
import com.timetable.timetable.domain.schedule.repository.ScheduledClassRepository;
import com.timetable.timetable.domain.schedule.repository.TimetableRepository;
import com.timetable.timetable.domain.user.entity.ApplicationUser;
import com.timetable.timetable.domain.user.service.UserService;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.transaction.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Serves the persisted timetable to the frontend.
 *
 * Response is shaped like TimetableSolution so the frontend store
 * and DTOs need zero changes.
 */
@RestController
@RequestMapping("/api/v1/timetables")
@RequiredArgsConstructor
public class TimetableLoadController {

    private final TimetableRepository timetableRepository;
    private final ScheduledClassRepository scheduledClassRepository;
    private final UserService userService;

    @GetMapping("/{year}/{semester}")
    @Transactional
    public ResponseEntity<?> getTimetable(
            @PathVariable int year,
            @PathVariable int semester) {

        Timetable timetable = timetableRepository
                .findByAcademicYearAndSemester(year, semester)
                .orElse(null);

        if (timetable == null) {
            return ResponseEntity.notFound().build();
        }

        List<ScheduledClass> classes = scheduledClassRepository
                .findAllWithDetailsByPeriod(year, semester);

        // Calculate workloads to determine overloaded status
        Map<Long, Integer> hoursPerTeacher = classes.stream()
                .map(ScheduledClass::getCohortSubject)
                .distinct()
                .collect(Collectors.groupingBy(
                        cs -> cs.getAssignedTeacher().getId(),
                        Collectors.summingInt(CohortSubject::getWeeklyHours)));

        List<Map<String, Object>> lessonAssignments = classes.stream()
                .map(sc -> toLessonAssignment(sc, hoursPerTeacher))
                .toList();

        return ResponseEntity.ok(Map.of(
                "id", timetable.getId(),
                "academicYear", timetable.getAcademicYear(),
                "semester", timetable.getSemester(),
                "status", timetable.getStatus().name(),
                "feasible", timetable.isFeasible(),
                "score", timetable.getScore() != null ? timetable.getScore() : "0hard/0soft",
                "totalLessons", lessonAssignments.size(),
                "unassignedLessons", 0,
                "lessonAssignments", lessonAssignments));
    }

    private static Map<String, Object> toLessonAssignment(ScheduledClass sc, Map<Long, Integer> workloadMap) {
        ApplicationUser teacher = sc.getTeacher();
        int totalHours = workloadMap.getOrDefault(teacher.getId(), 0);
        boolean overloaded = totalHours > com.timetable.timetable.domain.schedule.entity.AcademicPolicy
                .getWeeklyHoursLimit(teacher);

        return Map.of(
                "id", sc.getId(),
                "blockNumber", 0,
                "assigned", true,
                "timeslot", Map.of(
                        "id", sc.getTimeslot().getId(),
                        "dayOfWeek", sc.getTimeslot().getDayOfWeek().toString(),
                        "startTime", sc.getTimeslot().getStartTime().toString(),
                        "endTime", sc.getTimeslot().getEndTime().toString(),
                        "displayName", sc.getTimeslot().getStartTime() + " - " + sc.getTimeslot().getEndTime(),
                        "dayNumber", sc.getTimeslot().getDayOfWeek().getValue()),
                "room", Map.of(
                        "id", sc.getRoom().getId(),
                        "name", sc.getRoom().getName(),
                        "capacity", sc.getRoom().getCapacity()),
                "cohort", Map.of(
                        "id", sc.getCohort().getId(),
                        "displayName", sc.getCohort().getDisplayName(),
                        "studentCount", sc.getCohort().getStudentCount(),
                        "courseId", sc.getCohort().getCourse().getId(),
                        "year", sc.getCohort().getYear(),
                        "section", sc.getCohort().getSection()),
                "subject", Map.of(
                        "id", sc.getSubject().getId(),
                        "name", sc.getSubject().getName(),
                        "credits", sc.getSubject().getCredits(),
                        "targetYear", sc.getSubject().getTargetYear(),
                        "targetSemester", sc.getSubject().getTargetSemester()),
                "teacher", Map.of(
                        "id", teacher.getId(),
                        "name", teacher.getUsername(),
                        "fullName", teacher.getUsername(),
                        "email", teacher.getEmail(),
                        "overloaded", overloaded),
                "studentCount", sc.getCohort().getStudentCount(),
                "courseId", sc.getCohort().getCourse().getId());
    }

    @GetMapping("/me/student")
    @Transactional
    public ResponseEntity<?> getMyStudentTimetable(
            @RequestParam int year,
            @RequestParam int semester) {

        ApplicationUser user = userService.getAuthenticatedUser();

        List<ScheduledClass> allPeriodClasses = scheduledClassRepository
                .findAllWithDetailsByPeriod(year, semester);

        // Encontra a turma do estudante para o período
        List<ScheduledClass> classes = allPeriodClasses.stream()
                .filter(sc -> sc.getCohort().getStudents().contains(user))
                .toList();

        if (classes.isEmpty())
            return ResponseEntity.ok(Map.of(
                    "lessonAssignments", List.of(),
                    "feasible", true,
                    "score", "0hard/0soft",
                    "status", "PUBLISHED",
                    "totalLessons", 0,
                    "unassignedLessons", 0));

        Map<Long, Integer> hoursPerTeacher = allPeriodClasses.stream()
                .map(ScheduledClass::getCohortSubject)
                .distinct()
                .collect(Collectors.groupingBy(
                        cs -> cs.getAssignedTeacher().getId(),
                        Collectors.summingInt(CohortSubject::getWeeklyHours)));

        Timetable timetable = timetableRepository
                .findByAcademicYearAndSemester(year, semester)
                .orElseThrow();

        return ResponseEntity.ok(Map.of(
                "id", timetable.getId(),
                "academicYear", timetable.getAcademicYear(),
                "semester", timetable.getSemester(),
                "status", timetable.getStatus().name(),
                "feasible", true,
                "score", "0hard/0soft",
                "totalLessons", classes.size(),
                "unassignedLessons", 0,
                "lessonAssignments", classes.stream()
                        .map(sc -> toLessonAssignment(sc, hoursPerTeacher))
                        .toList()));
    }

    @GetMapping("/me/teacher")
    @Transactional
    public ResponseEntity<?> getMyTeacherTimetable(
            @RequestParam int year,
            @RequestParam int semester) {

        ApplicationUser user = userService.getAuthenticatedUser();

        List<ScheduledClass> allPeriodClasses = scheduledClassRepository
                .findAllWithDetailsByPeriod(year, semester);

        List<ScheduledClass> classes = allPeriodClasses.stream()
                .filter(sc -> sc.getTeacher().getId().equals(user.getId()))
                .toList();

        if (classes.isEmpty())
            return ResponseEntity.ok(Map.of(
                    "lessonAssignments", List.of(),
                    "feasible", true,
                    "score", "0hard/0soft",
                    "status", "PUBLISHED",
                    "totalLessons", 0,
                    "unassignedLessons", 0));

        Map<Long, Integer> hoursPerTeacher = allPeriodClasses.stream()
                .map(ScheduledClass::getCohortSubject)
                .distinct()
                .collect(Collectors.groupingBy(
                        cs -> cs.getAssignedTeacher().getId(),
                        Collectors.summingInt(CohortSubject::getWeeklyHours)));

        Timetable timetable = timetableRepository
                .findByAcademicYearAndSemester(year, semester)
                .orElseThrow();

        return ResponseEntity.ok(Map.of(
                "id", timetable.getId(),
                "academicYear", timetable.getAcademicYear(),
                "semester", timetable.getSemester(),
                "status", timetable.getStatus().name(),
                "feasible", timetable.getScore(),
                "score", timetable.isFeasible(),
                "totalLessons", classes.size(),
                "unassignedLessons", 0,
                "lessonAssignments", classes.stream()
                        .map(sc -> toLessonAssignment(sc, hoursPerTeacher))
                        .toList()));
    }
}
