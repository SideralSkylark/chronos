package com.timetable.timetable.domain.schedule.entity;

import java.time.DayOfWeek;
import java.util.Set;

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
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Represents an academic subject taught as part of a course, including its target year and semester.
 */
@Entity
@Table(name = "subjects")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Subject {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private int credits;

    @Column(nullable = false)
    private int targetYear;

    @Column(nullable = false)
    private int targetSemester;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "subject_teachers", joinColumns = @JoinColumn(name = "subject_id"), inverseJoinColumns = @JoinColumn(name = "teacher_id"))
    private Set<ApplicationUser> eligibleTeachers;

    @Column(nullable = false)
    @Builder.Default
    private boolean fixedDaySession = false;

    @Enumerated(EnumType.STRING)
    @Column(nullable = true)
    private DayOfWeek fixedDayOfWeek;
}
