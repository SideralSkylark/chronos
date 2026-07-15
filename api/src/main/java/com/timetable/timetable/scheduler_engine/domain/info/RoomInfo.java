package com.timetable.timetable.scheduler_engine.domain.info;

import com.timetable.timetable.domain.schedule.entity.TimePeriod;
import java.util.Map;
import java.util.Set;
import lombok.*;

/**
 * Represents a physical room where lessons can take place. This is a lightweight, immutable
 * representation for the solver.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoomInfo {

  /** Unique identifier from the Room entity */
  private Long id;

  /** Room name/number (e.g., "A101", "Lab 3") */
  private String name;

  /** Maximum number of students the room can accommodate */
  private int capacity;

  private Map<TimePeriod, Set<Long>> allowedCoursesByPeriod;

  /** Checks if this room can be used by a specific course in a specifig time period */
  public boolean isAvailableForCourse(Long courseId, TimePeriod period) {
    // Sem restrições = disponível para todos
    if (allowedCoursesByPeriod == null || allowedCoursesByPeriod.isEmpty()) {
      return true;
    }

    // Sem restrição para este período = disponível para todos neste período
    Set<Long> allowedCourses = allowedCoursesByPeriod.get(period);
    if (allowedCourses == null || allowedCourses.isEmpty()) {
      return true;
    }

    // Verificar se o curso está na lista permitida
    return allowedCourses.contains(courseId);
  }

  /** Checks if this room has sufficient capacity for a given number of students */
  public boolean hasSufficientCapacity(int studentCount) {
    return capacity >= studentCount;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof RoomInfo)) return false;
    RoomInfo that = (RoomInfo) o;
    return id != null && id.equals(that.id);
  }

  @Override
  public int hashCode() {
    return id != null ? id.hashCode() : 0;
  }

  @Override
  public String toString() {
    return "RoomInfo{" + "id=" + id + ", name='" + name + '\'' + ", capacity=" + capacity + '}';
  }
}
