package com.timetable.timetable.common.response;

import java.time.Instant;

import org.springframework.http.HttpStatus;

/**
 * Simple record for wrapping error responses in API replies.
 *
 * @param status    The http status code
 * @param message   The error message
 * @param path      The component that created the error
 * @param timestamp The time when it happened
 */
public record ErrorResponse(
        int status,
        String message,
        String path,
        Instant Timestamp) {
    public static ErrorResponse of(
            HttpStatus status,
            String message,
            String path) {
        return new ErrorResponse(
                status.value(),
                message,
                path,
                Instant.now());
    }
}
