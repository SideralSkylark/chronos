package com.timetable.timetable.domain.dashboard.service;

import com.timetable.timetable.domain.dashboard.dto.CourseRankDTO;
import com.timetable.timetable.domain.dashboard.dto.DashboardStatsDTO;
import com.timetable.timetable.domain.dashboard.dto.TeacherWorkloadDTO;
import com.timetable.timetable.domain.dashboard.dto.YearBreakdownDTO;
import com.timetable.timetable.domain.dashboard.dto.OversizedCohortDTO;
import com.timetable.timetable.domain.dashboard.dto.FragmentationRiskCohortDTO;
import com.timetable.timetable.domain.dashboard.dto.RoomTierDTO;
import com.timetable.timetable.domain.dashboard.dto.DistributionMismatchDTO;
import com.timetable.timetable.domain.schedule.entity.AcademicPolicy;
import com.timetable.timetable.domain.schedule.entity.Cohort;
import com.timetable.timetable.domain.schedule.entity.CohortSubject;
import com.timetable.timetable.domain.schedule.entity.Room;
import com.timetable.timetable.domain.schedule.entity.Timetable;
import com.timetable.timetable.domain.user.entity.ApplicationUser;
import com.timetable.timetable.domain.user.entity.UserRole;
import com.timetable.timetable.domain.schedule.repository.CohortRepository;
import com.timetable.timetable.domain.schedule.repository.CohortSubjectRepository;
import com.timetable.timetable.domain.schedule.repository.RoomRepository;
import com.timetable.timetable.domain.schedule.repository.TimetableRepository;
import com.timetable.timetable.domain.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
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
    private final TimetableRepository timetableRepository;

    public DashboardStatsDTO computeStats(Integer year, Integer semester) {
        // Determine period to analyze
        int targetYear;
        int targetSemester;

        if (year != null && semester != null) {
            targetYear = year;
            targetSemester = semester;
        } else {
            Timetable latest = timetableRepository.findFirstByOrderByAcademicYearDescSemesterDesc()
                    .orElse(null);
            if (latest != null) {
                targetYear = latest.getAcademicYear();
                targetSemester = latest.getSemester();
            } else {
                // Fallback to current year if no timetable exists
                targetYear = java.time.LocalDate.now().getYear();
                targetSemester = 1;
            }
        }

        List<Room> rooms = roomRepository.findAll();
        long totalRoomCount = rooms.size();
        long totalRoomCapacity = rooms.stream().mapToLong(Room::getCapacity).sum();

        List<Cohort> cohorts = cohortRepository.findByAcademicYearAndSemester(targetYear, targetSemester);

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
                .map(y -> new YearBreakdownDTO(y, cohortsByYearCount.get(y), studentsByYear.get(y)))
                .sorted(Comparator.comparing(YearBreakdownDTO::getYear))
                .toList();

        int bottleneckYear = studentsByYear.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(0);

        List<ApplicationUser> teachers = userRepository.findAllByRole(UserRole.TEACHER);
        long totalTeachers = teachers.size();

        List<CohortSubject> cohortSubjects = cohortSubjectRepository.findByAcademicYearAndSemester(targetYear,
                targetSemester);
        Map<ApplicationUser, Long> hoursPerTeacher = cohortSubjects.stream()
                .collect(Collectors.groupingBy(CohortSubject::getAssignedTeacher,
                        Collectors.summingLong(CohortSubject::getWeeklyHours)));

        long teachersOverloaded = teachers.stream().filter(t -> {
            long hours = hoursPerTeacher.getOrDefault(t, 0L);
            long max = AcademicPolicy.getWeeklyHoursLimit(t);
            return hours > max;
        }).count();

        long totalAssignedHours = hoursPerTeacher.values().stream().mapToLong(Long::longValue).sum();
        double avgHoursPerTeacher = totalTeachers > 0 ? (double) totalAssignedHours / totalTeachers : 0.0;

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
                .map(entry -> new CourseRankDTO(entry.getKey().getId(), entry.getKey().getName(),
                        entry.getValue().size()))
                .sorted(Comparator.comparing(CourseRankDTO::getCohortCount).reversed())
                .limit(5)
                .toList();

        // Teacher workload rankings
        List<TeacherWorkloadDTO> allTeacherWorkloads = teachers.stream()
                .map(t -> {
                    long hours = hoursPerTeacher.getOrDefault(t, 0L);
                    long max = AcademicPolicy.getWeeklyHoursLimit(t);
                    boolean overloaded = hours > max;
                    return new TeacherWorkloadDTO(t.getId(), t.getUsername(), hours, max, overloaded);
                })
                .toList();

        List<TeacherWorkloadDTO> mostLoadedTeachers = allTeacherWorkloads.stream()
                .sorted(Comparator.comparing(TeacherWorkloadDTO::getTotalHours).reversed())
                .limit(5)
                .toList();

        List<TeacherWorkloadDTO> leastLoadedTeachers = allTeacherWorkloads.stream()
                .filter(tw -> tw.getTotalHours() > 0)
                .sorted(Comparator.comparing(TeacherWorkloadDTO::getTotalHours))
                .limit(5)
                .toList();

        DashboardStatsDTO dto = new DashboardStatsDTO();
        dto.setAcademicYear(targetYear);
        dto.setSemester(targetSemester);

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
        dto.setTotalAssignedHours(totalAssignedHours);
        dto.setAvgHoursPerTeacher(avgHoursPerTeacher);
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

        // --- Feasibility Diagnostics ---
        int maxRoomCapacity = rooms.stream().mapToInt(Room::getCapacity).max().orElse(0);

        List<OversizedCohortDTO> oversizedCohorts = cohorts.stream()
                .filter(c -> c.getStudentCount() > maxRoomCapacity)
                .map(c -> {
                    int compatible = (int) rooms.stream().filter(r -> r.getCapacity() >= c.getStudentCount()).count();
                    return new OversizedCohortDTO(c.getId(), c.getDisplayName(), c.getStudentCount(),
                            c.getStudentCount(), compatible, "RED");
                })
                .toList();

        List<FragmentationRiskCohortDTO> fragmentationRisk = cohorts.stream()
                .filter(c -> {
                    int headcount = c.getStudentCount();
                    if (headcount == 0)
                        return false;
                    int maxCompCap = rooms.stream()
                            .mapToInt(Room::getCapacity)
                            .filter(cap -> cap >= headcount)
                            .max()
                            .orElse(0);

                    if (maxCompCap == 0)
                        return false;
                    double utilization = (double) headcount / maxCompCap * 100;
                    return utilization >= 85;
                })
                .map(c -> {
                    int headcount = c.getStudentCount();
                    int maxCompCap = rooms.stream()
                            .mapToInt(Room::getCapacity)
                            .filter(cap -> cap >= headcount)
                            .max()
                            .orElse(0);
                    double utilization = (double) headcount / maxCompCap * 100;
                    return new FragmentationRiskCohortDTO(c.getId(), c.getDisplayName(), headcount, maxCompCap,
                            utilization);
                })
                .toList();

        // Room Tiers
        long smallRooms = rooms.stream().filter(r -> r.getCapacity() <= 29).count();
        long mediumRooms = rooms.stream().filter(r -> r.getCapacity() >= 30 && r.getCapacity() <= 54).count();
        long largeRooms = rooms.stream().filter(r -> r.getCapacity() >= 55).count();

        long smallCohorts = cohorts.stream().filter(c -> c.getStudentCount() <= 29).count();
        long mediumCohorts = cohorts.stream().filter(c -> c.getStudentCount() >= 30 && c.getStudentCount() <= 54)
                .count();
        long largeCohorts = cohorts.stream().filter(c -> c.getStudentCount() >= 55).count();

        List<RoomTierDTO> roomTierDistribution = new ArrayList<>();
        double totalRoomsCount = (double) rooms.size();
        double totalCohortsCount = (double) cohorts.size();

        if (totalRoomsCount > 0 && totalCohortsCount > 0) {
            roomTierDistribution.add(new RoomTierDTO("Pequena (≤29)", smallRooms, (smallRooms / totalRoomsCount) * 100,
                    (smallCohorts / totalCohortsCount) * 100,
                    (smallRooms < smallCohorts) ? "YELLOW" : "GREEN"));
            roomTierDistribution.add(new RoomTierDTO("Média (30–54)", mediumRooms,
                    (mediumRooms / totalRoomsCount) * 100, (mediumCohorts / totalCohortsCount) * 100,
                    (mediumRooms < mediumCohorts) ? "RED" : "GREEN"));
            roomTierDistribution.add(new RoomTierDTO("Grande (≥55)", largeRooms, (largeRooms / totalRoomsCount) * 100,
                    (largeCohorts / totalCohortsCount) * 100,
                    (largeRooms < largeCohorts) ? "RED" : "GREEN"));
        }

        List<DistributionMismatchDTO> distributionMismatches = new ArrayList<>();
        if (totalRoomsCount > 0 && totalCohortsCount > 0) {
            distributionMismatches.add(new DistributionMismatchDTO("Pequena dimensão",
                    (smallCohorts / totalCohortsCount) * 100, (smallRooms / totalRoomsCount) * 100));
            distributionMismatches.add(new DistributionMismatchDTO("Média dimensão",
                    (mediumCohorts / totalCohortsCount) * 100, (mediumRooms / totalRoomsCount) * 100));
            distributionMismatches.add(new DistributionMismatchDTO("Grande dimensão",
                    (largeCohorts / totalCohortsCount) * 100, (largeRooms / totalRoomsCount) * 100));
        }

        dto.setOversizedCohorts(oversizedCohorts);
        dto.setFragmentationRisk(fragmentationRisk);
        dto.setRoomTierDistribution(roomTierDistribution);
        dto.setRoomScarcityNote(oversizedCohorts.isEmpty() ? "Não foram detectadas turmas sem sala compatível."
                : "Existem turmas que excedem a capacidade de todas as salas.");
        dto.setDistributionMismatches(distributionMismatches);
        dto.setDistributionMismatchNote(mediumRooms < mediumCohorts || largeRooms < largeCohorts
                ? "A oferta de salas grandes/médias é inferior à procura das turmas."
                : "A distribuição de salas parece equilibrada.");

        return dto;
    }
}
