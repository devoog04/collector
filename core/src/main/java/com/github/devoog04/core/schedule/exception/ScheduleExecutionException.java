package com.github.devoog04.core.schedule.exception;

import com.github.devoog04.core.definition.JobDefinitionKey;

public class ScheduleExecutionException extends JobSchedulerException {

    public ScheduleExecutionException(JobDefinitionKey key, String message) {
        super(key, message);
    }

    public ScheduleExecutionException(JobDefinitionKey key, String message, Throwable cause) {
        super(key, message, cause);
    }
}