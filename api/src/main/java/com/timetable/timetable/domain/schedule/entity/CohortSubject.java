package com.timetable.timetable.domain.schedule.entity;

import com.timetable.timetable.domain.user.entity.ApplicationUser;
import com.timetable.timetable.domain.user.entity.UserRole;

import jakarta.persistence.*;
import lombok.*;

/**
 * Binds a {@link Subject} to a specific @{link Cohort} with a
 * {@link UserRole.TEACHER}
 * Represents an instance of a lesson to be lectured
 */

@Entity
@Table(name = "cohort_subjects", uniqueConstraints = {
        @UniqueConstraint(columnNames = { "cohort_id", "subject_id", "academic_year", "semester" })
})
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CohortSubject {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cohort_id", nullable = false)
    private Cohort cohort;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_id", nullable = false)
    private Subject subject;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_teacher_id", nullable = false)
    private ApplicationUser assignedTeacher;

    @Column(nullable = false)
    private int academicYear;

    @Column(nullable = false)
    private int semester;

    @Column(nullable = false)
    @Builder.Default
    private boolean isActive = true;

    /** 2 sessões por semana — política institucional */
    public int getLessonBlocksPerWeek() {
        if (subject != null && subject.isFixedDaySession()) {
            return 3;
        }
        return AcademicPolicy.SESSIONS_PER_WEEK;
    }

    /** 4h/week of contact — used to calculate a teachers workload*/
    public int getWeeklyHours() {
        return AcademicPolicy.WEEKLY_CONTACT_HOURS;
    }

    public String getDisplayName() {
        return cohort.getDisplayName() + " - " + subject.getName() +
                " (" + assignedTeacher.getUsername() + ")";
    }

    /**
     * Checks if a teacher is eligible to lecture the lesson
     */
    public boolean isTeacherEligible() {
        // Phantom teachers are always considered eligible
        if (assignedTeacher.getUsername().startsWith("PHANTOM_")) {
            return true;
        }
        if (assignedTeacher.isSimulationTeam()) {
            return true;
        }
        return subject.getEligibleTeachers().contains(assignedTeacher);
    }

    /**
     * Validates data consistency
     */
    public boolean isValid() {
        // Checks year and semester alignment
        if (cohort.getAcademicYear() != academicYear || cohort.getSemester() != semester) {
            return false;
        }

        // Check if the subject is for the given semester
        if (subject.getTargetSemester() != semester) {
            return false;
        }

        // check teacher elegibility
        if (!isTeacherEligible()) {
            return false;
        }

        // Check if subject belongs to the cohorts course
        if (!subject.getCourse().equals(cohort.getCourse())) {
            return false;
        }

        return true;
    }

    @PrePersist
    @PreUpdate
    private void validate() {
        if (!isValid()) {
            throw new IllegalStateException(
                    "CohortSubject inválido: " + getDisplayName() +
                            " - Verifique alinhamento de ano/semestre, curso e elegibilidade do professor");
        }
    }
}
