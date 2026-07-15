package com.timetable.timetable.scheduler_engine.domain.info;

import com.timetable.timetable.domain.schedule.entity.TimePeriod;
import java.time.DayOfWeek;
import java.time.LocalTime;
import lombok.*;

/**
 * Represents a time slot when lessons can be scheduled. This is a lightweight, immutable
 * representation for the solver.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TimeslotInfo {

  /** Unique identifier from the Timeslot entity */
  private Long id;

  /** Day of the week (MONDAY, TUESDAY, etc.) */
  private DayOfWeek dayOfWeek;

  /** Start time (e.g., 08:00) */
  private LocalTime startTime;

  /** End time (e.g., 09:50 for a 110-minute block) */
  private LocalTime endTime;

  public TimePeriod getPeriod() {
    return TimePeriod.fromStartTime(startTime);
  }

  /** Returns a human-readable description (e.g., "MONDAY 08:00-09:50") */
  public String getDisplayName() {
    return dayOfWeek + " " + startTime + "-" + endTime;
  }

  /** Gets the day as an integer (1=Monday, 5=Friday) for sorting/comparisons */
  public int getDayNumber() {
    return dayOfWeek.getValue();
  }

  /** Checks if this timeslot is on the same day as another */
  public boolean isSameDay(TimeslotInfo other) {
    return other != null && this.dayOfWeek == other.dayOfWeek;
  }

  /** Checks if this timeslot overlaps with another */
  public boolean overlapsWith(TimeslotInfo other) {
    if (other == null || !isSameDay(other)) {
      return false;
    }
    return this.startTime.isBefore(other.endTime) && this.endTime.isAfter(other.startTime);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof TimeslotInfo)) return false;
    TimeslotInfo that = (TimeslotInfo) o;
    return id != null && id.equals(that.id);
  }

  @Override
  public int hashCode() {
    return id != null ? id.hashCode() : 0;
  }

  @Override
  public String toString() {
    return "TimeslotInfo{" + "id=" + id + ", " + getDisplayName() + '}';
  }
}
