package com.timetable.timetable.scheduler_engine.domain.info;

import lombok.*;

/**
 * Represents a teacher in the scheduling problem.
 * This is a lightweight, immutable representation for the solver.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TeacherInfo {
    
    /**
     * Unique identifier from the ApplicationUser entity
     */
    private Long id;
    
    /**
     * Teacher's name or username
     */
    private String name;
    
    /**
     * Teacher's full name (if different from username)
     */
    private String fullName;
    
    /**
     * Email address (optional, for reporting)
     */
    private String email;

    private boolean simulationTeam;

    private boolean overloaded;
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TeacherInfo)) return false;
        TeacherInfo that = (TeacherInfo) o;
        return id != null && id.equals(that.id);
    }
    
    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }
    
    @Override
    public String toString() {
        return "TeacherInfo{" +
                "id=" + id +
                ", name='" + name + '\'' +
                '}';
    }
}
