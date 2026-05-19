package com.timetable.timetable.auth.dto;

import java.time.LocalDateTime;

/**
 * Data Transfer Object representing an active user session.
 *
 * @param tokenId   The unique identifier of the session token.
 * @param ip        The IP address from which the session was initiated.
 * @param device    The device/User-Agent string associated with the session.
 * @param loginTime The timestamp when the session started.
 * @param active    Whether the session is currently valid and active.
 */
public record SessionDTO(
        Long tokenId,
        String ip,
        String device,
        LocalDateTime loginTime,
        boolean active) {
}
