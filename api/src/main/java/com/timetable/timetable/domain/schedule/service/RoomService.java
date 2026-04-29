package com.timetable.timetable.domain.schedule.service;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.timetable.timetable.domain.schedule.dto.CreateRoomRequest;
import com.timetable.timetable.domain.schedule.dto.RoomFilterParams;
import com.timetable.timetable.domain.schedule.dto.UpdateRoomRequest;
import com.timetable.timetable.domain.schedule.entity.Course;
import com.timetable.timetable.domain.schedule.entity.Room;
import com.timetable.timetable.domain.schedule.entity.RoomCourseRestriction;
import com.timetable.timetable.domain.schedule.entity.TimePeriod;
import com.timetable.timetable.domain.schedule.exception.RoomNotFoundException;
import com.timetable.timetable.domain.schedule.repository.RoomRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class RoomService {
    private final RoomRepository roomRepository;
    private final CourseService courseService;

    public Room createRoom(CreateRoomRequest roomRequest) {
        log.debug("Creating room");

        if (roomRepository.existsByName(roomRequest.name())) {
            log.error("There is already a room with the same name: {}", roomRequest.name());
            throw new IllegalStateException();
        }

        Room room = Room.builder()
            .name(roomRequest.name())
            .capacity(roomRequest.capacity())
            .restrictions(new HashSet<>())
            .build();

        addRestrictions(room, roomRequest.restrictedToCourseId(), roomRequest.periodRestrictions());

        Room saved = roomRepository.save(room);

        log.info("Room {} created", saved.getId());
        return saved;
    }

    public int findMaxCapacity() {
        return roomRepository.findMaxCapacity();
    }

    @Transactional
    public Page<Room> getAll(Pageable pageable, RoomFilterParams filters) {
        if (filters == null) {
            return roomRepository.findAll(pageable);
        }

        Specification<Room> spec = (root, query, cb) -> {
            query.distinct(true);
            List<Predicate> predicates = new ArrayList<>();

            if (filters.getName() != null && !filters.getName().isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("name")), "%" + filters.getName().toLowerCase() + "%"));
            }

            if (filters.getCapacityMin() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("capacity"), filters.getCapacityMin()));
            }

            if (filters.getCapacityMax() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("capacity"), filters.getCapacityMax()));
            }

            if (filters.getCourseId() != null || filters.getPeriod() != null) {
                Join<Room, RoomCourseRestriction> restrictionsJoin = root.join("restrictions");

                if (filters.getCourseId() != null) {
                    predicates.add(cb.equal(restrictionsJoin.get("course").get("id"), filters.getCourseId()));
                }

                if (filters.getPeriod() != null) {
                    predicates.add(cb.equal(restrictionsJoin.get("period"), filters.getPeriod()));
                }
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return roomRepository.findAll(spec, pageable);
    }

    @Transactional
    public Page<Room> getAll(Pageable pageable) {
        return getAll(pageable, null);
    }

    @Transactional
    public Room getById(Long id) {
        log.debug("Looking for room: {}", id);
        Room room = roomRepository.findById(id)
            .orElseThrow(() -> new RoomNotFoundException("Room not found."));

        log.info("Room {} found", id);
        return room;
    }

    @Transactional
    public Room updateRoom(Long id, UpdateRoomRequest updateRequest) {
        log.debug("Updating room {}", id);
        Room room = getById(id);

        if (!room.getName().equals(updateRequest.name()) && roomRepository.existsByName(updateRequest.name())) {
            log.warn("Another room with the same name already exists");
            throw new IllegalArgumentException("Another room with that name already exists.");
        }

        room.setName(updateRequest.name());
        room.setCapacity(updateRequest.capacity());
        // Clear existing restrictions first to avoid duplicate unique-key conflicts on re-insert
        room.getRestrictions().clear();
        roomRepository.save(room);
        roomRepository.flush();

        // refresh reference so we have a managed collection after flush
        room = getById(id);
        addRestrictions(room, updateRequest.restrictedToCourseId(), updateRequest.periodRestrictions());

        Room saved = roomRepository.save(room);

        log.info("Updated room {}", id);
        return saved;
    }

    public void deleteRoom(Long id) {
        log.debug("Deleting room {}", id);
        if (!roomRepository.existsById(id)) {
            throw new RoomNotFoundException("Room not found.");
        }

        roomRepository.deleteById(id);
        log.info("Room {} deleted", id);
    }

    /**
     * Helper: Adiciona restrições à sala
     * Prioriza periodRestrictions, senão usa restrictedToCourseId
     */
    private void addRestrictions(Room room, Long singleCourseId,
                                 Map<TimePeriod, Set<Long>> periodRestrictions) {

        // CASO 1: Restrições granulares por período (nova API)
        if (periodRestrictions != null && !periodRestrictions.isEmpty()) {
            for (Map.Entry<TimePeriod, Set<Long>> entry : periodRestrictions.entrySet()) {
                TimePeriod period = entry.getKey();
                Set<Long> courseIds = entry.getValue();

                for (Long courseId : courseIds) {
                    // Check if restriction already exists to avoid duplicates
                    boolean alreadyExists = room.getRestrictions().stream()
                        .anyMatch(r -> r.getCourse().getId().equals(courseId) && r.getPeriod() == period);

                    if (!alreadyExists) {
                        Course course = courseService.getById(courseId);

                        RoomCourseRestriction restriction = RoomCourseRestriction.builder()
                            .room(room)
                            .course(course)
                            .period(period)
                            .build();

                        room.getRestrictions().add(restriction);
                    }
                }
            }
            log.debug("Added {} granular restrictions", room.getRestrictions().size());
            return;
        }

        // CASO 2: Curso único para todos os períodos (retrocompatibilidade)
        if (singleCourseId != null) {
            Course course = courseService.getById(singleCourseId);

            for (TimePeriod period : TimePeriod.values()) {
                RoomCourseRestriction restriction = RoomCourseRestriction.builder()
                    .room(room)
                    .course(course)
                    .period(period)
                    .build();

                room.getRestrictions().add(restriction);
            }
            log.debug("Added course restriction for all periods: {}", course.getName());
            return;
        }

        // CASO 3: Sem restrições (sala disponível para todos)
        log.debug("No restrictions - room available for all courses in all periods");
    }
}
