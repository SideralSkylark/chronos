//
// import static org.assertj.core.api.Assertions.*;
// import static org.mockito.ArgumentMatchers.*;
// import static org.mockito.Mockito.*;
//
// import java.util.*;
//
// import org.junit.jupiter.api.BeforeEach;
// import org.junit.jupiter.api.DisplayName;
// import org.junit.jupiter.api.Test;
// import org.junit.jupiter.api.extension.ExtendWith;
// import org.mockito.InjectMocks;
// import org.mockito.Mock;
// import org.mockito.junit.jupiter.MockitoExtension;
//
// import com.timetable.timetable.domain.schedule.dto.CreateCohortRequest;
// import com.timetable.timetable.domain.schedule.dto.UpdateCohortRequest;
// import com.timetable.timetable.domain.schedule.entity.Cohort;
// import com.timetable.timetable.domain.schedule.entity.Course;
// import com.timetable.timetable.domain.schedule.exception.CohortNotFoundException;
// import com.timetable.timetable.domain.schedule.repository.CohortRepository;
// import com.timetable.timetable.domain.schedule.service.CohortService;
// import com.timetable.timetable.domain.schedule.service.CourseService;
// import com.timetable.timetable.domain.user.entity.ApplicationUser;
// import com.timetable.timetable.domain.user.entity.UserRole;
// import com.timetable.timetable.domain.user.entity.UserRoleEntity;
// import com.timetable.timetable.domain.user.service.UserService;
//
// @ExtendWith(MockitoExtension.class)
// @DisplayName("CohortService Tests")
// class CohortServiceTest {
//
//     @Mock
//     private CohortRepository cohortRepository;
//
//     @Mock
//     private CourseService courseService;
//
//     @Mock
//     private UserService userService;
//
//     @InjectMocks
//     private CohortService cohortService;
//
//     private Course course;
//     private ApplicationUser student1;
//     private ApplicationUser student2;
//     private ApplicationUser nonStudent;
//     private UserRoleEntity teacherRole;
//     private UserRoleEntity studentRole;
//
//     @BeforeEach
//     void setUp() {
//         course = Course.builder()
//             .id(1L)
//             .name("Computer Science")
//             .build();
//
//         teacherRole = new UserRoleEntity(1L, UserRole.TEACHER);
//         studentRole = new UserRoleEntity(2L, UserRole.STUDENT);
//
//         student1 = ApplicationUser.builder()
//             .id(1L)
//             .username("student1")
//             .roles(Set.of(studentRole))
//             .build();
//
//         student2 = ApplicationUser.builder()
//             .id(2L)
//             .username("student2")
//             .roles(Set.of(studentRole))
//             .build();
//
//         nonStudent = ApplicationUser.builder()
//             .id(3L)
//             .username("teacher1")
//             .roles(Set.of(teacherRole))
//             .build();
//     }
//
//     @Test
//     @DisplayName("Should create cohort successfully")
//     void shouldCreateCohortSuccessfully() {
//         CreateCohortRequest request = new CreateCohortRequest(
//             1, "A", 1, 2024, 1L, List.of(1L, 2L)
//         );
//
//         when(cohortRepository.existsByYearAndSectionAndSemesterAndAcademicYearAndCourseId(
//             eq(1), eq("A"), eq(1), eq(2024), eq(1L)
//         )).thenReturn(false);
//         when(courseService.getById(1L)).thenReturn(course);
//         when(userService.findUserOrThrow(1L)).thenReturn(student1);
//         when(userService.findUserOrThrow(2L)).thenReturn(student2);
//         when(cohortRepository.save(any(Cohort.class))).thenAnswer(inv -> {
//             Cohort c = inv.getArgument(0);
//             c.setId(1L);
//             return c;
//         });
//
//         Cohort result = cohortService.createCohort(request);
//
//         assertThat(result).isNotNull();
//         assertThat(result.getYear()).isEqualTo(1);
//         assertThat(result.getSection()).isEqualTo("A");
//         assertThat(result.getSemester()).isEqualTo(1);
//         assertThat(result.getAcademicYear()).isEqualTo(2024);
//         assertThat(result.getCourse()).isEqualTo(course);
//         assertThat(result.getStudents()).hasSize(2);
//
//         verify(cohortRepository).save(any(Cohort.class));
//     }
//
//     @Test
//     @DisplayName("Should create cohort without students")
//     void shouldCreateCohortWithoutStudents() {
//         CreateCohortRequest request = new CreateCohortRequest(
//             1, "A", 1, 2024, 1L, null
//         );
//
//         when(cohortRepository.existsByYearAndSectionAndSemesterAndAcademicYearAndCourseId(
//             anyInt(), anyString(), anyInt(), anyInt(), anyLong()
//         )).thenReturn(false);
//         when(courseService.getById(1L)).thenReturn(course);
//         when(cohortRepository.save(any(Cohort.class))).thenAnswer(inv -> {
//             Cohort c = inv.getArgument(0);
//             c.setId(1L);
//             return c;
//         });
//
//         Cohort result = cohortService.createCohort(request);
//
//         assertThat(result).isNotNull();
//         assertThat(result.getStudents()).isEmpty();
//         verify(userService, never()).findUserOrThrow(anyLong());
//     }
//
//     @Test
//     @DisplayName("Should throw exception when cohort already exists")
//     void shouldThrowExceptionWhenCohortExists() {
//         CreateCohortRequest request = new CreateCohortRequest(
//             1, "A", 1, 2024, 1L, null
//         );
//
//         when(cohortRepository.existsByYearAndSectionAndSemesterAndAcademicYearAndCourseId(
//             1, "A", 1, 2024, 1L
//         )).thenReturn(true);
//
//         assertThatThrownBy(() -> cohortService.createCohort(request))
//             .isInstanceOf(IllegalStateException.class)
//             .hasMessageContaining("already exists");
//
//         verify(cohortRepository, never()).save(any(Cohort.class));
//     }
//
//     @Test
//     @DisplayName("Should throw exception when student user is not a student")
//     void shouldThrowExceptionWhenUserIsNotStudent() {
//         CreateCohortRequest request = new CreateCohortRequest(
//             1, "A", 1, 2024, 1L, List.of(3L)
//         );
//
//         when(cohortRepository.existsByYearAndSectionAndSemesterAndAcademicYearAndCourseId(
//             anyInt(), anyString(), anyInt(), anyInt(), anyLong()
//         )).thenReturn(false);
//         when(courseService.getById(1L)).thenReturn(course);
//         when(userService.findUserOrThrow(3L)).thenReturn(nonStudent);
//
//         assertThatThrownBy(() -> cohortService.createCohort(request))
//             .isInstanceOf(IllegalArgumentException.class)
//             .hasMessageContaining("is not a student");
//     }
//
//     @Test
//     @DisplayName("Should get cohort by id successfully")
//     void shouldGetCohortByIdSuccessfully() {
//         Cohort cohort = Cohort.builder()
//             .id(1L)
//             .year(1)
//             .section("A")
//             .build();
//
//         when(cohortRepository.findById(1L)).thenReturn(Optional.of(cohort));
//
//         Cohort result = cohortService.getById(1L);
//
//         assertThat(result).isNotNull();
//         assertThat(result.getId()).isEqualTo(1L);
//         verify(cohortRepository).findById(1L);
//     }
//
//     @Test
//     @DisplayName("Should throw exception when cohort not found by id")
//     void shouldThrowExceptionWhenCohortNotFoundById() {
//         when(cohortRepository.findById(1L)).thenReturn(Optional.empty());
//
//         assertThatThrownBy(() -> cohortService.getById(1L))
//             .isInstanceOf(CohortNotFoundException.class)
//             .hasMessageContaining("not found");
//     }
//
//     @Test
//     @DisplayName("Should get cohorts by course")
//     void shouldGetCohortsByCourse() {
//         List<Cohort> cohorts = Arrays.asList(
//             Cohort.builder().id(1L).course(course).build(),
//             Cohort.builder().id(2L).course(course).build()
//         );
//
//         when(cohortRepository.findByCourseId(1L)).thenReturn(cohorts);
//
//         List<Cohort> result = cohortService.getCohortsByCourse(1L);
//
//         assertThat(result).hasSize(2);
//         verify(cohortRepository).findByCourseId(1L);
//     }
//
//     @Test
//     @DisplayName("Should get cohorts by year and semester")
//     void shouldGetCohortsByYearAndSemester() {
//         List<Cohort> cohorts = Arrays.asList(
//             Cohort.builder().id(1L).year(1).semester(1).build(),
//             Cohort.builder().id(2L).year(1).semester(1).build()
//         );
//
//         when(cohortRepository.findByYearAndSemester(1, 1)).thenReturn(cohorts);
//
//         List<Cohort> result = cohortService.getCohortsByYearAndSemester(1, 1);
//
//         assertThat(result).hasSize(2);
//         verify(cohortRepository).findByYearAndSemester(1, 1);
//     }
//
//     @Test
//     @DisplayName("Should get cohorts by academic year")
//     void shouldGetCohortsByAcademicYear() {
//         List<Cohort> cohorts = Arrays.asList(
//             Cohort.builder().id(1L).academicYear(2024).build(),
//             Cohort.builder().id(2L).academicYear(2024).build()
//         );
//
//         when(cohortRepository.findByAcademicYear(2024)).thenReturn(cohorts);
//
//         List<Cohort> result = cohortService.getCohortsByAcademicYear(2024);
//
//         assertThat(result).hasSize(2);
//         verify(cohortRepository).findByAcademicYear(2024);
//     }
//
//     @Test
//     @DisplayName("Should update cohort successfully")
//     void shouldUpdateCohortSuccessfully() {
//         UpdateCohortRequest request = new UpdateCohortRequest(
//             2, "B", 2, 2025, List.of(1L)
//         );
//
//         Cohort existingCohort = Cohort.builder()
//             .id(1L)
//             .year(1)
//             .section("A")
//             .semester(1)
//             .academicYear(2024)
//             .course(course)
//             .students(new HashSet<>())
//             .build();
//
//         when(cohortRepository.findById(1L)).thenReturn(Optional.of(existingCohort));
//         when(cohortRepository.existsAnotherWithSameAttributes(
//             anyInt(), anyString(), anyInt(), anyInt(), anyLong(), anyLong()
//         )).thenReturn(false);
//         when(userService.findUserOrThrow(1L)).thenReturn(student1);
//         when(cohortRepository.save(any(Cohort.class))).thenAnswer(inv -> inv.getArgument(0));
//
//         Cohort result = cohortService.updateCohort(1L, request);
//
//         assertThat(result).isNotNull();
//         assertThat(result.getYear()).isEqualTo(2);
//         assertThat(result.getSection()).isEqualTo("B");
//         assertThat(result.getSemester()).isEqualTo(2);
//         assertThat(result.getAcademicYear()).isEqualTo(2025);
//         assertThat(result.getStudents()).hasSize(1);
//
//         verify(cohortRepository).save(existingCohort);
//     }
//
//     @Test
//     @DisplayName("Should throw exception when updating to duplicate cohort")
//     void shouldThrowExceptionWhenUpdatingToDuplicate() {
//         UpdateCohortRequest request = new UpdateCohortRequest(
//             2, "B", 2, 2025, List.of()
//         );
//
//         Cohort existingCohort = Cohort.builder()
//             .id(1L)
//             .year(1)
//             .section("A")
//             .course(course)
//             .build();
//
//         when(cohortRepository.findById(1L)).thenReturn(Optional.of(existingCohort));
//         when(cohortRepository.existsAnotherWithSameAttributes(
//             2, "B", 2, 2025, 1L, 1L
//         )).thenReturn(true);
//
//         assertThatThrownBy(() -> cohortService.updateCohort(1L, request))
//             .isInstanceOf(IllegalArgumentException.class)
//             .hasMessageContaining("already exists");
//
//         verify(cohortRepository, never()).save(any(Cohort.class));
//     }
//
//     @Test
//     @DisplayName("Should throw exception when updating non-existent cohort")
//     void shouldThrowExceptionWhenUpdatingNonExistentCohort() {
//         UpdateCohortRequest request = new UpdateCohortRequest(
//             2, "B", 2, 2025, List.of()
//         );
//
//         when(cohortRepository.findById(1L)).thenReturn(Optional.empty());
//
//         assertThatThrownBy(() -> cohortService.updateCohort(1L, request))
//             .isInstanceOf(CohortNotFoundException.class);
//     }
//
//     @Test
//     @DisplayName("Should delete cohort successfully")
//     void shouldDeleteCohortSuccessfully() {
//         when(cohortRepository.existsById(1L)).thenReturn(true);
//         doNothing().when(cohortRepository).deleteById(1L);
//
//         cohortService.deleteCohort(1L);
//
//         verify(cohortRepository).deleteById(1L);
//     }
//
//     @Test
//     @DisplayName("Should throw exception when deleting non-existent cohort")
//     void shouldThrowExceptionWhenDeletingNonExistentCohort() {
//         when(cohortRepository.existsById(1L)).thenReturn(false);
//
//         assertThatThrownBy(() -> cohortService.deleteCohort(1L))
//             .isInstanceOf(CohortNotFoundException.class)
//             .hasMessageContaining("not found");
//
//         verify(cohortRepository, never()).deleteById(anyLong());
//     }
// }
