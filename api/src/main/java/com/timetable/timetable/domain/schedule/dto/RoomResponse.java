package com.timetable.timetable.domain.schedule.dto;

import com.timetable.timetable.domain.schedule.entity.Room;
import java.util.Set;
import java.util.stream.Collectors;

public record RoomResponse(
    Long id, String name, int capacity, Set<RoomRestrictionResponse> restrictions) {
  public static RoomResponse from(Room room) {
    return new RoomResponse(
        room.getId(),
        room.getName(),
        room.getCapacity(),
        room.getRestrictions().stream()
            .map(RoomRestrictionResponse::from)
            .collect(Collectors.toSet()));
  }
}
