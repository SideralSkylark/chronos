package com.timetable.timetable.domain.schedule.exception;

import com.timetable.timetable.common.exception.ResourceAlreadyExistsException;

public class CohortAlreadyConfirmedException extends ResourceAlreadyExistsException {
    public CohortAlreadyConfirmedException(String message) {
        super(message);
    }
}
