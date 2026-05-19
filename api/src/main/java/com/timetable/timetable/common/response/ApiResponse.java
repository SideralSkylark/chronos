package com.timetable.timetable.common.response;

import java.time.Instant;

/**
 * Simple record for wrapping responses in API replies.
 *
 * @param data      Generic data
 * @param message   The message from the response
 * @param timestamp The time when it happened
 */
public record ApiResponse<T>(
        T data,
        String message,
        Instant timestamp) {
    public ApiResponse(T data, String message) {
        this(data, message, Instant.now());
    }
}
