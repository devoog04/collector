package com.github.devoog04.core.definition;

import com.github.devoog04.core.schedule.AbstractJobScheduler;

import java.util.Objects;

/**
 * {@link JobDefinition}을 고유하게 식별하기 위한 키(Key) 인터페이스입니다.
 * <p>이 인터페이스의 구현체는 작업의 그룹, 이름 등을 조합하여 유일성을 보장한다.</p>
 */
public interface JobDefinitionKey {

    /**
     * 두 키의 논리적 동일성을 비교합니다.
     * @param o 비교할 대상 객체
     * @return 내부 식별 값(예: ID, Name)이 일치하면 true
     */
    @Override
    boolean equals(Object o);

    /**
     * 객체의 해시코드 값을 반환합니다.
     * equals가 true를 반환하는 두 객체는 반드시 동일한 hashCode를 가져야 합니다.
     * @return 해시 버킷 위치 결정을 위한 정수 값
     */
    @Override
    int hashCode();
}