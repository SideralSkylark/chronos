package com.timetable.timetable.auth.exception;

import com.timetable.timetable.common.exception.ResourceAlreadyExistsException;

public class UserAlreadyExistsException extends ResourceAlreadyExistsException {
    public UserAlreadyExistsException(String message) {
        super(message);
    }
}
