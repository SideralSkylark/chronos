// package com.timetable.timetable.sheduler_engine.solver;
//
// import java.time.LocalDate;
// import java.time.LocalTime;
//
// import com.timetable.timetable.scheduler_engine.domain.LessonAssignment;
// import com.timetable.timetable.scheduler_engine.domain.LocalTimeSlot;
// import com.timetable.timetable.scheduler_engine.domain.TfCohort;
// import com.timetable.timetable.scheduler_engine.domain.TfTeacher;
// import com.timetable.timetable.scheduler_engine.domain.TimetableSolution;
// import com.timetable.timetable.scheduler_engine.solver.TimetableConstraintProvider;
//
// import org.junit.jupiter.api.Test; 
// import ai.timefold.solver.test.api.score.stream.ConstraintVerifier;
//
// class TimetableConstraintProviderTest {
//
//     private final ConstraintVerifier<TimetableConstraintProvider, TimetableSolution>
//         constraintVerifier = ConstraintVerifier.build(
//             new TimetableConstraintProvider(),
//             TimetableSolution.class,
//             LessonAssignment.class
//         );
//
//     @Test
//     void teacherConflict_penalizes_whenSameTeacherSameTime() {
//         TfTeacher teacher = new TfTeacher(1L, "Alice");
//         TfCohort cohort1 = new TfCohort(1L, "A");
//         TfCohort cohort2 = new TfCohort(2L, "B");
//
//         LocalTimeSlot slot = new LocalTimeSlot(
//             LocalDate.now(),
//             LocalTime.of(8, 0),
//             LocalTime.of(9, 0)
//         );
//
//         LessonAssignment l1 = new LessonAssignment(
//             1L, null, teacher, cohort1, null, slot
//         );
//         LessonAssignment l2 = new LessonAssignment(
//             2L, null, teacher, cohort2, null, slot
//         );
//
//         constraintVerifier
//             .verifyThat(TimetableConstraintProvider::teacherConflict)
//             .given(l1, l2)
//             .penalizesBy(1);
//     }
// }
