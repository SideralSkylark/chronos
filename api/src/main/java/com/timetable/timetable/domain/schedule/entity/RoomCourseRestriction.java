package com.timetable.timetable.domain.schedule.entity;

import jakarta.persistence.*;
import lombok.*;

/** Define que uma sala está disponível para um curso em um período específico */
@Entity
@Table(
    name = "room_course_restrictions",
    uniqueConstraints = {@UniqueConstraint(columnNames = {"room_id", "course_id", "period"})})
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RoomCourseRestriction {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "room_id", nullable = false)
  private Room room;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "course_id", nullable = false)
  private Course course;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private TimePeriod period;
}
