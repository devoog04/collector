package com.github.devoog04.core.definition;

/**
 * 실행할 작업(Job)의 고유 정의를 나타내는 인터페이스입니다.
 */
public interface JobDefinition {
    /**
     * 이 작업 정의의 고유 식별자를 조회합니다.
     * 해당 키는 로깅, 통계, 중복 방지 로직에서 참조됩니다.
     */
    JobDefinitionKey getKey();

    Runnable getRunnable();
}