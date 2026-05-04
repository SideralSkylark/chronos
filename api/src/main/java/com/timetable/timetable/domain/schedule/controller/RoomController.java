package com.timetable.timetable.domain.schedule.controller;

import com.timetable.timetable.common.response.ApiResponse;
import com.timetable.timetable.common.response.ResponseFactory;
import com.timetable.timetable.domain.schedule.dto.CreateRoomRequest;
import com.timetable.timetable.domain.schedule.dto.RoomFilterParams;
import com.timetable.timetable.domain.schedule.dto.RoomResponse;
import com.timetable.timetable.domain.schedule.dto.UpdateRoomRequest;
import com.timetable.timetable.domain.schedule.entity.TimePeriod;
import com.timetable.timetable.domain.schedule.service.RoomService;

import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedModel;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("api/v1/rooms")
public class RoomController {
    private final RoomService roomService;

    @PostMapping
    public ResponseEntity<ApiResponse<RoomResponse>> create(@Valid @RequestBody CreateRoomRequest request) {
        return ResponseFactory.ok(
                roomService.createRoom(request),
                "Room created successfully.");
    }

    @GetMapping("/max-capacity")
    public ResponseEntity<ApiResponse<Integer>> getMaxCapacity() {
        return ResponseFactory.ok(
                roomService.findMaxCapacity(),
                "Max capacity fetched.");
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PagedModel<RoomResponse>>> getAll(
            Pageable pageable,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Integer capacityMin,
            @RequestParam(required = false) Integer capacityMax,
            @RequestParam(required = false) Long courseId,
            @RequestParam(required = false) TimePeriod period) {

        RoomFilterParams filters = new RoomFilterParams();
        filters.setName(name);
        filters.setCapacityMin(capacityMin);
        filters.setCapacityMax(capacityMax);
        filters.setCourseId(courseId);
        filters.setPeriod(period);

        return ResponseFactory.ok(
                new PagedModel<>(roomService.getAll(pageable, filters)),
                "Rooms fetched successfully.");
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<RoomResponse>> getById(@PathVariable Long id) {
        return ResponseFactory.ok(
                roomService.getById(id),
                "Room fetched successfully.");
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<RoomResponse>> update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateRoomRequest request) {
        return ResponseFactory.ok(
                roomService.updateRoom(id, request),
                "Room updated successfully.");
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        roomService.deleteRoom(id);
        return ResponseEntity.noContent().build();
    }
}
