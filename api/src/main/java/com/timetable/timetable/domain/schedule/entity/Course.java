package com.timetable.timetable.domain.schedule.entity;

import java.time.LocalDateTime;
import java.util.Map;

import com.timetable.timetable.domain.user.entity.ApplicationUser;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Represents an academic course (e.g., Computer Science) overseen by a coordinator.
 */
@Entity
@Table(name = "courses")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Course {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "coordinator_id")
    private ApplicationUser coordinator;

    /**
     * duration of the course in years
     */
    @Column(nullable = false)
    @Builder.Default
    private int years = 4;

    /**
     * Maximum number of cohorts per semester for a given academic year.
     * Key = academic year (1..n)
     * Value = maximum cohorts allowed per semester
     */
    @ElementCollection
    @CollectionTable(name = "course_expected_cohorts", joinColumns = @JoinColumn(name = "course_id"))
    @MapKeyColumn(name = "academic_year")
    @Column(name = "expected_cohorts", nullable = false)
    private Map<Integer, Integer> expectedCohortsPerAcademicYear;

    @Column(nullable = false)
    @Builder.Default
    private boolean hasBusinessSimulation = false;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime UpdatedAt;
}
