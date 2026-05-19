package com.timetable.timetable.common.response;

/**
 * Simple record for wrapping message responses in API replies.
 * Used for success and error messages.
 *
 * @param message The message to return
 */
public record MessageResponse(String message) {
}
