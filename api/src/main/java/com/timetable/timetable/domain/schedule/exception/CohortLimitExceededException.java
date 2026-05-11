package com.timetable.timetable.domain.schedule.exception;

import com.timetable.timetable.common.exception.ResourceAlreadyExistsException;

public class CohortLimitExceededException extends ResourceAlreadyExistsException {
    public CohortLimitExceededException(String message) {
        super(message);
    }
}
