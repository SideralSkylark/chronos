package com.timetable.timetable.domain.dashboard.service;

import com.timetable.timetable.domain.dashboard.dto.CourseRankDTO;
import com.timetable.timetable.domain.dashboard.dto.DashboardStatsDTO;
import com.timetable.timetable.domain.dashboard.dto.TeacherWorkloadDTO;
import com.timetable.timetable.domain.dashboard.dto.YearBreakdownDTO;
import com.timetable.timetable.domain.schedule.entity.Cohort;
import com.timetable.timetable.domain.schedule.entity.CohortSubject;
import com.timetable.timetable.domain.schedule.entity.Room;
import com.timetable.timetable.domain.user.entity.ApplicationUser;
import com.timetable.timetable.domain.user.entity.UserRole;
import com.timetable.timetable.domain.schedule.repository.CohortRepository;
import com.timetable.timetable.domain.schedule.repository.CohortSubjectRepository;
import com.timetable.timetable.domain.schedule.repository.RoomRepository;
import com.timetable.timetable.domain.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardStatsService {

    private final RoomRepository roomRepository;
    private final CohortRepository cohortRepository;
    private final UserRepository userRepository;
    private final CohortSubjectRepository cohortSubjectRepository;

    public DashboardStatsDTO computeStats() {
        List<Room> rooms = roomRepository.findAll();
        long totalRoomCount = rooms.size();
        long totalRoomCapacity = rooms.stream().mapToLong(Room::getCapacity).sum();

        List<Cohort> cohorts = cohortRepository.findAll();
        long totalCohortCount = cohorts.size();
        long totalCohortDemand = cohorts.stream().mapToLong(Cohort::getStudentCount).sum();
        long largestCohort = cohorts.stream().mapToLong(Cohort::getStudentCount).max().orElse(0);
        long smallestCohort = cohorts.stream().mapToLong(Cohort::getStudentCount).min().orElse(0);
        double averageCohortSize = totalCohortCount > 0 ? (double) totalCohortDemand / totalCohortCount : 0.0;

        Map<Integer, Long> cohortsByYearCount = cohorts.stream()
                .collect(Collectors.groupingBy(Cohort::getYear, Collectors.counting()));
        Map<Integer, Long> studentsByYear = cohorts.stream()
                .collect(Collectors.groupingBy(Cohort::getYear, Collectors.summingLong(Cohort::getStudentCount)));

        List<YearBreakdownDTO> cohortsByYear = cohortsByYearCount.keySet().stream()
                .map(year -> new YearBreakdownDTO(year, cohortsByYearCount.get(year), studentsByYear.get(year)))
                .sorted(Comparator.comparing(YearBreakdownDTO::getYear))
                .toList();

        int bottleneckYear = studentsByYear.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(0);

        List<ApplicationUser> teachers = userRepository.findAllByRole(UserRole.TEACHER);
        long totalTeachers = teachers.size();

        List<CohortSubject> cohortSubjects = cohortSubjectRepository.findAll();
        Map<ApplicationUser, Long> slotsPerTeacher = cohortSubjects.stream()
                .collect(Collectors.groupingBy(CohortSubject::getAssignedTeacher,
                        Collectors.summingLong(CohortSubject::getLessonBlocksPerWeek)));

        long teachersOverloaded = teachers.stream().filter(t -> {
            long slots = slotsPerTeacher.getOrDefault(t, 0L);
            long max = t.getWeeklyHoursLimit() > 0 ? t.getWeeklyHoursLimit() : 20;
            return slots > max;
        }).count();

        long totalAssignedSlots = slotsPerTeacher.values().stream().mapToLong(Long::longValue).sum();
        double avgSlotsPerTeacher = totalTeachers > 0 ? (double) totalAssignedSlots / totalTeachers : 0.0;

        long capacityMargin = totalRoomCapacity - totalCohortDemand;
        String solverReadiness;
        String solverReadinessReason;

        if (totalRoomCapacity < totalCohortDemand) {
            solverReadiness = "RED";
            solverReadinessReason = "Inviável — sem capacidade";
        } else if (totalRoomCapacity < totalCohortDemand * 1.2) {
            solverReadiness = "YELLOW";
            solverReadinessReason = "Verificar configuração";
        } else {
            solverReadiness = "GREEN";
            solverReadinessReason = "Capacidade adequada para as turmas estimadas";
        }

        // Shift logic
        Set<Integer> morningYears = Set.of(1, 3, 5);
        Set<Integer> afternoonYears = Set.of(2, 4);

        long morningDemand = cohorts.stream()
                .filter(c -> morningYears.contains(c.getYear()))
                .mapToLong(Cohort::getStudentCount)
                .sum();

        long afternoonDemand = cohorts.stream()
                .filter(c -> afternoonYears.contains(c.getYear()))
                .mapToLong(Cohort::getStudentCount)
                .sum();

        String morningReadiness;
        String morningReadinessReason;
        if (totalRoomCapacity < morningDemand) {
            morningReadiness = "RED";
            morningReadinessReason = "Inviável — sem capacidade";
        } else if (totalRoomCapacity < morningDemand * 1.2) {
            morningReadiness = "YELLOW";
            morningReadinessReason = "Verificar configuração";
        } else {
            morningReadiness = "GREEN";
            morningReadinessReason = "Capacidade adequada para as turmas estimadas";
        }

        String afternoonReadiness;
        String afternoonReadinessReason;
        if (totalRoomCapacity < afternoonDemand) {
            afternoonReadiness = "RED";
            afternoonReadinessReason = "Inviável — sem capacidade";
        } else if (totalRoomCapacity < afternoonDemand * 1.2) {
            afternoonReadiness = "YELLOW";
            afternoonReadinessReason = "Verificar configuração";
        } else {
            afternoonReadiness = "GREEN";
            afternoonReadinessReason = "Capacidade adequada para as turmas estimadas";
        }

        // Top Courses
        List<CourseRankDTO> topCoursesByCohorts = cohorts.stream()
                .filter(c -> c.getCourse() != null)
                .collect(Collectors.groupingBy(c -> c.getCourse()))
                .entrySet().stream()
                .map(entry -> new CourseRankDTO(entry.getKey().getId(), entry.getKey().getName(), entry.getValue().size()))
                .sorted(Comparator.comparing(CourseRankDTO::getCohortCount).reversed())
                .limit(5)
                .toList();

        // Teacher workload rankings
        List<TeacherWorkloadDTO> allTeacherWorkloads = teachers.stream()
                .map(t -> {
                    long slots = slotsPerTeacher.getOrDefault(t, 0L);
                    long max = t.getWeeklyHoursLimit() > 0 ? t.getWeeklyHoursLimit() : 20;
                    boolean overloaded = slots > max;
                    return new TeacherWorkloadDTO(t.getId(), t.getUsername(), slots, max, overloaded);
                })
                .toList();

        List<TeacherWorkloadDTO> mostLoadedTeachers = allTeacherWorkloads.stream()
                .sorted(Comparator.comparing(TeacherWorkloadDTO::getTotalSlots).reversed())
                .limit(5)
                .toList();

        List<TeacherWorkloadDTO> leastLoadedTeachers = allTeacherWorkloads.stream()
                .filter(tw -> tw.getTotalSlots() > 0)
                .sorted(Comparator.comparing(TeacherWorkloadDTO::getTotalSlots))
                .limit(5)
                .toList();

        DashboardStatsDTO dto = new DashboardStatsDTO();
        dto.setTotalRoomCapacity(totalRoomCapacity);
        dto.setTotalRoomCount(totalRoomCount);
        dto.setTotalCohortDemand(totalCohortDemand);
        dto.setTotalCohortCount(totalCohortCount);
        dto.setLargestCohort(largestCohort);
        dto.setSmallestCohort(smallestCohort);
        dto.setAverageCohortSize(averageCohortSize);
        dto.setBottleneckYear(bottleneckYear);
        dto.setCohortsByYear(cohortsByYear);
        dto.setTotalTeachers(totalTeachers);
        dto.setTeachersOverloaded(teachersOverloaded);
        dto.setTotalAssignedSlots(totalAssignedSlots);
        dto.setAvgSlotsPerTeacher(avgSlotsPerTeacher);
        dto.setSolverReadiness(solverReadiness);
        dto.setSolverReadinessReason(solverReadinessReason);
        dto.setCapacityMargin(capacityMargin);
        
        dto.setMorningDemand(morningDemand);
        dto.setAfternoonDemand(afternoonDemand);
        dto.setMorningReadiness(morningReadiness);
        dto.setAfternoonReadiness(afternoonReadiness);
        dto.setMorningReadinessReason(morningReadinessReason);
        dto.setAfternoonReadinessReason(afternoonReadinessReason);

        dto.setTopCoursesByCohorts(topCoursesByCohorts);
        dto.setMostLoadedTeachers(mostLoadedTeachers);
        dto.setLeastLoadedTeachers(leastLoadedTeachers);

        return dto;
    }
}