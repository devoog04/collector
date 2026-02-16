package com.github.devoog04.core.schedule.exception;

import com.github.devoog04.core.definition.JobDefinitionKey;

public class NotFoundScheduleException extends JobSchedulerException {

    public NotFoundScheduleException(JobDefinitionKey key) {
        super(key, "An event with that key does not already exist.");
    }

    public NotFoundScheduleException(JobDefinitionKey key, Throwable cause) {
        super(key, "An event with that key does not already exist.", cause);
    }
}