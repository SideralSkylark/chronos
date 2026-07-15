package com.timetable.timetable.domain.schedule.entity;

import com.timetable.timetable.domain.user.entity.ApplicationUser;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.HashSet;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Represents a group of students belonging to a specific course, year, and section. */
@Entity
@Table(name = "cohorts")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Cohort {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  /** Field for the students current year ie: 1, 2, 3 (for 1st, 2nd, 3rd) */
  @Column(nullable = false)
  private int year;

  /** ie: A, B, C, .. */
  @Column(nullable = false)
  private String section;

  /** Field for the cohorts current academic year. ie: 2026 */
  @Column(nullable = false)
  private int academicYear;

  @Column(nullable = false)
  private int semester;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "course_id", nullable = false)
  private Course course;

  @Column(nullable = false)
  private String courseNameSnapshot;

  @ManyToMany
  @JoinTable(
      name = "cohort_students",
      joinColumns = @JoinColumn(name = "cohort_id"),
      inverseJoinColumns = @JoinColumn(name = "student_id"))
  @Builder.Default
  private Set<ApplicationUser> students = new HashSet<>();

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  @Builder.Default
  private CohortStatus status = CohortStatus.ESTIMATED;

  @Column(nullable = false)
  @Builder.Default
  private int estimatedStudentCount = AcademicPolicy.ESTIMATED_STUDENT_COUNT;

  public int getStudentCount() {
    if (status == CohortStatus.CONFIRMED && !students.isEmpty()) {
      return students.size();
    }

    return estimatedStudentCount;
  }

  public String getDisplayName() {
    return year + "ano" + "-" + courseNameSnapshot + "-" + section + "-" + academicYear;
  }
}
