package com.timetable.timetable.domain.schedule.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.HashSet;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "optional_groups")
public class OptionalGroup {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private String name;

  @Builder.Default
  @OneToMany(mappedBy = "optionalGroup", fetch = FetchType.LAZY)
  private Set<Subject> subjects = new HashSet<>();

  public void addSubject(Subject subject) {
    subjects.add(subject);
    subject.setOptionalGroup(this);
  }

  public void removeSubject(Subject subject) {
    subjects.remove(subject);
    subject.setOptionalGroup(null);
  }
}
