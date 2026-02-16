package com.github.devoog04.core.schedule;

import com.github.devoog04.core.definition.JobDefinition;
import com.github.devoog04.core.definition.JobDefinitionKey;
import com.github.devoog04.core.schedule.exception.*;

/**
 * 작업(Job)의 실행 주기를 관리하고 스케줄링을 담당하는 인터페이스입니다.
 * 모든 구현체는 멀티스레드 환경에서 스레드 안전성(Thread-safety)을 보장해야 하며,
 * 각 메서드는 원자적(Atomic)으로 동작하여 상태 불일치를 방지해야 합니다.
 */
public interface JobScheduler {

    /**
     * 지정된 정책에 따라 작업을 스케줄러에 등록합니다.
     * <p><b>원자성 및 예외 규정:</b>
     * <ul>
     * <li>동일한 키의 작업이 이미 존재할 경우 {@link DuplicateScheduleException}을 발생시켜야 합니다.</li>
     * <li>"중복 체크"와 "실제 등록"은 원자적으로 수행되어야 하며, 등록 실패 시 컨테이너 상태가 변경되어서는 안 됩니다.</li>
     * </ul>
     * @param definition     작업 식별 정보 및 실행 로직을 담은 객체
     * @param schedulePolicy 적용할 스케줄 정책
     * @throws DuplicateScheduleException 이미 동일한 키가 등록된 경우
     * @throws ScheduleExecutionException 작업 스케줄 등록 과정에서 치명적 오류가 발생한 경우
     */
    void schedule(JobDefinition definition, SchedulePolicy schedulePolicy);

    /**
     * 기존에 등록된 작업의 스케줄 정책을 변경합니다.
     * <p><b>복구 및 일관성 보장:</b>
     * <ul>
     * <li>기존 작업의 취소와 새 정책의 등록은 하나의 트랜잭션으로 처리되어야 합니다.</li>
     * <li>새 정책 등록 실패 시, 시스템은 기존 작업을 자동으로 복구(Rollback)하여 서비스 연속성을 유지해야 합니다.</li>
     * <li>복구마저 실패할 경우, 해당 작업을 컨테이너에서 격리하고 치명적 예외를 발생시켜 관리자의 개입을 유도해야 합니다.</li>
     * </ul>
     * @param key       대상 작업의 고유 키
     * @param newPolicy 새로 적용할 스케줄 정책
     * @throws NotFoundScheduleException  대상 키가 존재하지 않을 경우
     * @throws ScheduleExecutionException 작업 스케줄 중단 또는 등록 과정에서 치명적 오류가 발생한 경우
     */
    void reschedule(JobDefinitionKey key, SchedulePolicy newPolicy);

    /**
     * 스케줄러에서 특정 작업을 제거하고 즉시 중지합니다.
     * <p><b>원자성 보장:</b>
     * <ul>
     * <li>관리 목록에서의 제거와 실제 실행 중인 작업 스케줄 중단(Cancel)이 일관되게 처리되어야 합니다.</li>
     * <li>이미 존재하지 않는 키에 대한 요청은 무시하거나 경고 로그를 남기며, 예외를 던지지 않는 멱등성(Idempotency)을 가질 수 있습니다.</li>
     * </ul>
     * @param key 대상 작업의 고유 키
     * @throws ScheduleExecutionException 작업 스케줄 중단 과정에서 치명적 오류가 발생한 경우
     */
    void unschedule(JobDefinitionKey key);
}