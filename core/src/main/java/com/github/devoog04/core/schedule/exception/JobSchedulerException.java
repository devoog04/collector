package com.github.devoog04.core.schedule.exception;

import com.github.devoog04.core.definition.JobDefinitionKey;
import lombok.Getter;

/**
 * 스케줄링 처리 중 발생하는 모든 예외의 최상위 클래스입니다.
 * <p>발생한 에러의 대상이 되는 {@link JobDefinitionKey}를 필수로 포함하여
 * 로깅 및 장애 추적 시 정확한 문맥(Context)을 제공합니다.</p>
 */
@Getter
public class JobSchedulerException extends RuntimeException {
    private final JobDefinitionKey key;

    protected JobSchedulerException(JobDefinitionKey key, String message) {
        super(String.format("[Schedule] key = %s, message = %s", key, message));
        this.key = key;
    }

    protected JobSchedulerException(JobDefinitionKey key, String message, Throwable cause) {
        super(String.format("[Schedule] key = %s, message = %s", key, message), cause);
        this.key = key;
    }
}