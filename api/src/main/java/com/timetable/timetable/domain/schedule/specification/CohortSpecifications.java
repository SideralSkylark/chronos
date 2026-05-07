package com.timetable.timetable.domain.schedule.specification;

import com.timetable.timetable.domain.schedule.dto.CohortFilterParams;
import com.timetable.timetable.domain.schedule.entity.Cohort;
import org.springframework.data.jpa.domain.Specification;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;

public class CohortSpecifications {

    public static Specification<Cohort> withFilters(CohortFilterParams f) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (f.getName() != null && !f.getName().isBlank()) {
                String pattern = "%" + f.getName().toLowerCase() + "%";
                List<Predicate> searchPredicates = new ArrayList<>();

                searchPredicates.add(cb.like(cb.lower(root.get("courseNameSnapshot")), pattern));
                searchPredicates.add(cb.like(cb.lower(root.get("section")), pattern));

                try {
                    int yearValue = Integer.parseInt(f.getName().trim());
                    searchPredicates.add(cb.equal(root.get("year"), yearValue));
                } catch (NumberFormatException ignored) {
                    // not a number, skip year predicate
                }

                predicates.add(cb.or(searchPredicates.toArray(new Predicate[0])));
            }

            if (f.getCourseId() != null) {
                predicates.add(cb.equal(root.get("course").get("id"), f.getCourseId()));
            }

            if (f.getAcademicYear() != null) {
                predicates.add(cb.equal(root.get("academicYear"), f.getAcademicYear()));
            }

            if (f.getSemester() != null) {
                predicates.add(cb.equal(root.get("semester"), f.getSemester()));
            }

            if (f.getStatus() != null) {
                predicates.add(cb.equal(root.get("status"), f.getStatus()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
