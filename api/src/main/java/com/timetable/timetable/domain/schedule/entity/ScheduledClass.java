package com.timetable.timetable.domain.schedule.entity;

import com.timetable.timetable.domain.user.entity.ApplicationUser;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Represents a specific class session assigned to a room and timeslot within a timetable. */
@Entity
@Table(name = "scheduled_classes")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ScheduledClass {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "cohort_subject_id", nullable = false)
  private CohortSubject cohortSubject;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "timetable_id")
  private Timetable timetable;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "room_id")
  private Room room;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "timeslot_id")
  private Timeslot timeslot;

  @Column(nullable = false)
  @Builder.Default
  private boolean pinned = false;

  // TODO: this dosent look good
  public Subject getSubject() {
    return cohortSubject.getSubject();
  }

  public ApplicationUser getTeacher() {
    return cohortSubject.getAssignedTeacher();
  }

  public Cohort getCohort() {
    return cohortSubject.getCohort();
  }
}
