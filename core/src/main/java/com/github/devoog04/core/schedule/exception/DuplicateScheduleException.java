package com.github.devoog04.core.schedule.exception;

import com.github.devoog04.core.definition.JobDefinitionKey;

public class DuplicateScheduleException extends JobSchedulerException {

    public DuplicateScheduleException(JobDefinitionKey key) {
        super(key, "A schedule with the same key already exists.");
    }

    public DuplicateScheduleException(JobDefinitionKey key, Throwable cause) {
        super(key, "A schedule with the same key already exists.", cause);
    }
}