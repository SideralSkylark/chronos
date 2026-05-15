package com.timetable.timetable.scheduler_engine.mapper;

import com.timetable.timetable.domain.schedule.entity.*;
import com.timetable.timetable.scheduler_engine.domain.*;
import com.timetable.timetable.scheduler_engine.domain.info.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Maps between JPA persistence entities and solver domain objects.
 * 
 * Conversion flow:
 * - JPA entities → Solver domain (toPlanningProblem)
 * - Solver domain → JPA entities (toScheduledClasses)
 */
@Component
@Slf4j
public class TimetableSolutionMapper {

    // ========================================
    // MAIN MAPPING METHODS
    // ========================================

    /**
     * Converts JPA entities into a TimetableSolution ready for the solver.
     * 
     * This method:
     * 1. Converts all timeslots and rooms into Info objects (problem facts)
     * 2. For each CohortSubject, generates N empty LessonAssignments
     * (where N = blocks needed per week)
     * 3. Returns a TimetableSolution with unassigned planning variables
     * 
     * @param cohortSubjects Active cohort-subject combinations for this semester
     * @param timeslots      Available time slots for scheduling
     * @param rooms          Available rooms
     * @param academicYear   The academic year (e.g., 2026)
     * @param semester       The semester (1 or 2)
     * @return TimetableSolution ready to be solved
     */
    public TimetableSolution toPlanningProblem(
            List<CohortSubject> cohortSubjects,
            List<Timeslot> timeslots,
            List<Room> rooms,
            int academicYear,
            int semester) {

        log.info("Converting {} CohortSubjects, {} timeslots, {} rooms to planning problem",
                cohortSubjects.size(), timeslots.size(), rooms.size());

        // Pre-calculate workloads to mark overloaded teachers
        Map<Long, Integer> hoursPerTeacher = cohortSubjects.stream()
                .collect(Collectors.groupingBy(
                        cs -> cs.getAssignedTeacher().getId(),
                        Collectors.summingInt(CohortSubject::getWeeklyHours)));

        // Convert problem facts (resources)
        List<TimeslotInfo> timeslotInfos = timeslots.stream()
                .map(this::toTimeslotInfo)
                .collect(Collectors.toList());

        List<RoomInfo> roomInfos = rooms.stream()
                .map(this::toRoomInfo)
                .collect(Collectors.toList());

        // Generate lesson assignments (planning entities)
        List<LessonAssignment> lessonAssignments = new ArrayList<>();
        long lessonIdCounter = 1;

        for (CohortSubject cs : cohortSubjects) {
            int blocksNeeded = cs.getLessonBlocksPerWeek();
            CohortSubjectInfo csInfo = toCohortSubjectInfo(cs, hoursPerTeacher);

            log.debug("CohortSubject #{}: {} needs {} blocks/week",
                    cs.getId(), csInfo.getDisplayName(), blocksNeeded);

            // Create one LessonAssignment for each block needed
            for (int blockNumber = 1; blockNumber <= blocksNeeded; blockNumber++) {
                LessonAssignment lesson = LessonAssignment.builder()
                        .id(lessonIdCounter++)
                        .cohortSubject(csInfo)
                        .blockNumber(blockNumber)
                        .timeslot(null) // To be assigned by solver
                        .room(null) // To be assigned by solver
                        .build();

                lessonAssignments.add(lesson);
            }
        }

        log.info("Generated {} lesson assignments from {} cohort-subjects",
                lessonAssignments.size(), cohortSubjects.size());

        return TimetableSolution.builder()
                .lessonAssignments(lessonAssignments)
                .availableTimeslots(timeslotInfos)
                .availableRooms(roomInfos)
                .academicYear(academicYear)
                .semester(semester)
                .score(null) // Will be calculated by solver
                .build();
    }

    /**
     * Converts a solved TimetableSolution back into JPA ScheduledClass entities.
     * 
     * This method:
     * 1. Takes each LessonAssignment with assigned timeslot and room
     * 2. Looks up the corresponding JPA entities by ID
     * 3. Creates ScheduledClass entities linking everything together
     * 
     * @param solution  The solved timetable
     * @param timetable The Timetable entity to associate with
     * @return List of ScheduledClass entities ready to be persisted
     */
    public List<ScheduledClass> toScheduledClasses(
            TimetableSolution solution,
            Timetable timetable) {

        if (solution == null || solution.getLessonAssignments() == null) {
            log.warn("Cannot convert null solution to ScheduledClasses");
            return new ArrayList<>();
        }

        log.info("Converting {} lesson assignments to ScheduledClass entities",
                solution.getLessonAssignments().size());

        // Note: In a real implementation, you would inject repositories here
        // and fetch the actual JPA entities by ID. For now, this creates
        // ScheduledClass objects with the IDs that need to be resolved.

        List<ScheduledClass> scheduledClasses = new ArrayList<>();

        for (LessonAssignment lesson : solution.getLessonAssignments()) {
            if (lesson.getTimeslot() == null || lesson.getRoom() == null) {
                log.warn("Skipping unassigned lesson: {}", lesson.getDisplayName());
                continue;
            }

            // Create ScheduledClass with IDs
            // In real implementation, replace with:
            // cohortSubjectRepo.getReferenceById(lesson.getCohortSubject().getId())
            // timeslotRepo.getReferenceById(lesson.getTimeslot().getId())
            // roomRepo.getReferenceById(lesson.getRoom().getId())

            ScheduledClass scheduledClass = ScheduledClass.builder()
                    .timetable(timetable)
                    // .cohortSubject(cohortSubjectRepo.getReferenceById(...))
                    // .timeslot(timeslotRepo.getReferenceById(...))
                    // .room(roomRepo.getReferenceById(...))
                    .build();

            // Store IDs as metadata for now (you'll replace this)
            // scheduledClass.setCohortSubject(...);
            // scheduledClass.setTimeslot(...);
            // scheduledClass.setRoom(...);

            scheduledClasses.add(scheduledClass);
        }

        log.info("Created {} ScheduledClass entities", scheduledClasses.size());

        return scheduledClasses;
    }

    /**
     * Reconstructs a TimetableSolution from persisted ScheduledClass entities.
     *
     * Unlike toPlanningProblem() which creates empty (unassigned)
     * LessonAssignments,
     * this method produces LessonAssignments that already have timeslot and room
     * set —
     * representing the current state of the timetable as saved in the database.
     *
     * Used exclusively by PermutationService for constraint validation via
     * ScoreManager.
     *
     * @param scheduledClasses fully-fetched ScheduledClass rows (with all
     *                         associations loaded)
     * @param timeslots        all available timeslots (for the value range)
     * @param rooms            all available rooms (for the value range)
     * @param academicYear     metadata
     * @param semester         metadata
     */
    public TimetableSolution fromScheduledClasses(
            List<ScheduledClass> scheduledClasses,
            List<Timeslot> timeslots,
            List<Room> rooms,
            int academicYear,
            int semester) {

        log.info("Rebuilding TimetableSolution from {} persisted ScheduledClasses", scheduledClasses.size());

        List<TimeslotInfo> timeslotInfos = timeslots.stream()
                .map(this::toTimeslotInfo)
                .collect(Collectors.toList());

        List<RoomInfo> roomInfos = rooms.stream()
                .map(this::toRoomInfo)
                .collect(Collectors.toList());

        // Pre-calculate workloads
        Map<Long, Integer> hoursPerTeacher = scheduledClasses.stream()
                .map(ScheduledClass::getCohortSubject)
                .distinct()
                .collect(Collectors.groupingBy(
                        cs -> cs.getAssignedTeacher().getId(),
                        Collectors.summingInt(CohortSubject::getWeeklyHours)));

        // Build a lookup so we can set TimeslotInfo/RoomInfo instances from the
        // value-range lists.
        // ScoreManager requires the planning variable values to be the SAME object
        // instances
        // that appear in the value range — otherwise constraints that compare by
        // reference break.
        Map<Long, TimeslotInfo> timeslotById = timeslotInfos.stream()
                .collect(Collectors.toMap(TimeslotInfo::getId, Function.identity()));

        Map<Long, RoomInfo> roomById = roomInfos.stream()
                .collect(Collectors.toMap(RoomInfo::getId, Function.identity()));

        List<LessonAssignment> lessons = new ArrayList<>();

        for (ScheduledClass sc : scheduledClasses) {
            CohortSubjectInfo csInfo = toCohortSubjectInfo(sc.getCohortSubject(), hoursPerTeacher);

            // Use the value-range instance (same object reference) — critical for
            // ScoreManager
            TimeslotInfo timeslot = timeslotById.get(sc.getTimeslot().getId());
            RoomInfo room = roomById.get(sc.getRoom().getId());

            if (timeslot == null || room == null) {
                log.warn("Skipping ScheduledClass id={}: timeslot or room not in value range", sc.getId());
                continue;
            }

            LessonAssignment lesson = LessonAssignment.builder()
                    .id(sc.getId()) // use ScheduledClass.id as PlanningId
                    .cohortSubject(csInfo)
                    .blockNumber(0) // block number not needed for permutation validation
                    .timeslot(timeslot)
                    .room(room)
                    .pinned(sc.isPinned())
                    .build();

            lessons.add(lesson);
        }

        log.info("Rebuilt {} LessonAssignments", lessons.size());

        return TimetableSolution.builder()
                .lessonAssignments(lessons)
                .availableTimeslots(timeslotInfos)
                .availableRooms(roomInfos)
                .academicYear(academicYear)
                .semester(semester)
                .score(null) // ScoreManager will calculate
                .build();
    }

    // ========================================
    // ENTITY → INFO CONVERSIONS
    // ========================================

    public CohortSubjectInfo toCohortSubjectInfo(CohortSubject cs) {
        return toCohortSubjectInfo(cs, null);
    }

    public CohortSubjectInfo toCohortSubjectInfo(CohortSubject cs, Map<Long, Integer> workloadMap) {
        return CohortSubjectInfo.builder()
                .id(cs.getId())
                .cohort(toCohortInfo(cs.getCohort()))
                .subject(toSubjectInfo(cs.getSubject()))
                .teacher(toTeacherInfo(cs.getAssignedTeacher(), workloadMap))
                .lessonBlocksPerWeek(cs.getLessonBlocksPerWeek())
                .build();
    }

    private CohortInfo toCohortInfo(Cohort cohort) {
        return CohortInfo.builder()
                .id(cohort.getId())
                .displayName(cohort.getDisplayName())
                .studentCount(cohort.getStudentCount())
                .courseId(cohort.getCourse().getId())
                .year(cohort.getYear())
                .section(cohort.getSection())
                .build();
    }

    private SubjectInfo toSubjectInfo(Subject subject) {
        return SubjectInfo.builder()
                .id(subject.getId())
                .name(subject.getName())
                .credits(subject.getCredits())
                .targetYear(subject.getTargetYear())
                .targetSemester(subject.getTargetSemester())
                .fixedDaySession(subject.isFixedDaySession()) // ← NOVO
                .build();
    }

    private TeacherInfo toTeacherInfo(com.timetable.timetable.domain.user.entity.ApplicationUser teacher) {
        return toTeacherInfo(teacher, null);
    }

    private TeacherInfo toTeacherInfo(com.timetable.timetable.domain.user.entity.ApplicationUser teacher, Map<Long, Integer> workloadMap) {
        int totalHours = workloadMap != null ? workloadMap.getOrDefault(teacher.getId(), 0) : 0;
        boolean overloaded = totalHours > AcademicPolicy.getWeeklyHoursLimit(teacher);

        return TeacherInfo.builder()
                .id(teacher.getId())
                .name(teacher.getUsername())
                .fullName(teacher.getUsername())
                .email(teacher.getEmail())
                .simulationTeam(teacher.isSimulationTeam())
                .overloaded(overloaded)
                .build();
    }

    private TimeslotInfo toTimeslotInfo(Timeslot timeslot) {
        return TimeslotInfo.builder()
                .id(timeslot.getId())
                .dayOfWeek(timeslot.getDayOfWeek())
                .startTime(timeslot.getStartTime())
                .endTime(timeslot.getEndTime())
                .build();
    }

    private RoomInfo toRoomInfo(Room room) {
        Map<TimePeriod, Set<Long>> restrictionsMap = new HashMap<>();

        if (room.getRestrictions() != null && !room.getRestrictions().isEmpty()) {
            for (RoomCourseRestriction restriction : room.getRestrictions()) {
                restrictionsMap
                        .computeIfAbsent(restriction.getPeriod(), k -> new HashSet<>())
                        .add(restriction.getCourse().getId());
            }
        }

        return RoomInfo.builder()
                .id(room.getId())
                .name(room.getName())
                .capacity(room.getCapacity())
                .allowedCoursesByPeriod(restrictionsMap.isEmpty() ? null : restrictionsMap)
                .build();
    }
}
