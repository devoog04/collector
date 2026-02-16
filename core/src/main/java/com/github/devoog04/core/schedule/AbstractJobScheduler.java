package com.github.devoog04.core.schedule;

import com.github.devoog04.core.definition.JobDefinition;
import com.github.devoog04.core.definition.JobDefinitionKey;
import com.github.devoog04.core.schedule.exception.DuplicateScheduleException;
import com.github.devoog04.core.schedule.exception.JobSchedulerException;
import com.github.devoog04.core.schedule.exception.NotFoundScheduleException;
import com.github.devoog04.core.schedule.exception.ScheduleExecutionException;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * {@link JobScheduler} 인터페이스의 추상 구현체로, 공통적인 스케줄 관리 로직을 제공합니다.
 * <p>이 클래스는 다음과 같은 핵심 설계 원칙을 따릅니다:
 * <ul>
 * <li><b>스레드 안전성:</b> {@link ConcurrentHashMap}과 {@code compute} 계열 메서드를 사용하여
 * 멀티스레드 환경에서 작업 관리의 원자성을 보장합니다.</li>
 * <li><b>상태 일관성:</b> 스케줄러의 관리 목록(Map)과 실제 실행 상태(Running Task) 간의
 * 일관성을 유지하기 위해 예외 발생 시 복구 로직을 수행합니다.</li>
 * <li><b>멱등성:</b> 동일한 작업에 대한 중복 취소나 등록 요청을 안전하게 처리합니다.</li>
 * </ul>
 * @see JobScheduler
 * @see ScheduleManager
 */
@Slf4j
@RequiredArgsConstructor
public abstract class AbstractJobScheduler implements JobScheduler {
    /**
     * 활성화된 작업들을 관리하는 컨테이너입니다.
     * - Key: 작업을 식별하는 고유 키 (JobDefinitionKey), Value: 현재 스케줄러에 등록되어 정상 동작 중인 매니저 객체 (ScheduleManager)
     */
    protected final Map<JobDefinitionKey, ScheduleManager> container = new ConcurrentHashMap<>();

    @Override
    public final void schedule(JobDefinition definition, SchedulePolicy policy) {
        container.compute(definition.getKey(), (k, oldManager) -> {
            // 기존 manager 미존재
            if (oldManager == null) {
                try {
                    ScheduleManager manager = createManager(definition, policy);
                    log.info("schedule log 1");
                    manager.schedule();
                    return manager; // 신규 manager 등록
                } catch (Exception e) {
                    log.error("schedule log 2");
                    throw e; // 신규 manager 등록 X
                }
            }else {
                log.error("schedule log 3");
                throw new DuplicateScheduleException(definition.getKey());
            }
        });
    }

    @Override
    public void reschedule(JobDefinitionKey key, SchedulePolicy newPolicy) {
        AtomicReference<JobSchedulerException> exceptionRef = new AtomicReference<>();

        container.compute(key, (k, oldManager) -> {
            // 기존 manager 존재
            if (oldManager != null) {
                // 기존 manager 취소
                try {
                    log.info("reschedule log 1");
                    oldManager.cancel();
                } catch (Exception e) {
                    log.error("reschedule log 2");
                    throw e; // 기존 manager 유지
                }

                // 신규 manager 스케줄
                try {
                    ScheduleManager manager = createManager(oldManager.getJobDefinition(), newPolicy);
                    log.info("reschedule log 3");
                    manager.schedule();
                    return manager; // 신규 manager로 교체
                } catch (Exception e) {
                    log.error("reschedule log 4");

                    // 복구 : 기존 manager 다시 스케줄
                    try {
                        log.info("reschedule log 5");
                        oldManager.schedule();

                    } catch (Exception rollbackEx) {
                        // 복구 실패
                        log.error("reschedule log 6");
                        JobSchedulerException exception = new ScheduleExecutionException(key, "Critical failure: Reschedule failed and subsequent Rollback also failed for job . The job is now in a STOPPED state.", rollbackEx);
                        exceptionRef.set(exception);
                        return null; // 기존 manager 제거 // 기존, 신규 manager 모두 등록 X 상태
                    }

                    throw e; // 기존 manager 유지
                }
            } else {
                log.error("reschedule log 7");
                throw new NotFoundScheduleException(key);
            }
        });

        if (exceptionRef.get() != null) {
            throw exceptionRef.get();
        }
    }

    @Override
    public void unschedule(JobDefinitionKey key) {
        container.compute(key, (k, oldManager) -> {
            // 기존 manager 존재
            if (oldManager != null) {
                try {
                    // 기존 manager 취소
                    log.info("unschedule log 1");
                    oldManager.cancel();
                    return null; // 기존 manager 제거
                } catch (Exception e) {
                    log.error("unschedule log 2");
                    throw e; // 기존 manager 유지
                }
            } else {
                    log.warn("unschedule log 3");
                    return null;
            }
        });
    }

    /**
     * 플랫폼별 구체적인 스케줄링 메커니즘을 구현합니다 (예: Spring, Quartz 등).
     * @param definition 작업 정의
     * @param policy 정책
     * @return 생성된 {@link ScheduleManager}
     */
    abstract protected ScheduleManager createManager(JobDefinition definition, SchedulePolicy policy) throws ScheduleExecutionException;

    @Getter
    @RequiredArgsConstructor
    protected abstract static class ScheduleManager {
        private final AtomicBoolean scheduled = new AtomicBoolean(false);
        private final JobDefinition jobDefinition;
        private final SchedulePolicy policy;

        public final void schedule() throws ScheduleExecutionException {
            if (scheduled.compareAndSet(false, true)) {
                try {
                    doSchedule();
                } catch (Exception e) {
                    scheduled.set(false);
                    throw e;
                }
            }
        }

        public final void cancel() throws ScheduleExecutionException {
            if (scheduled.compareAndSet(true, false)) {
                try {
                    doCancel();
                } catch (Exception e) {
                    scheduled.set(true);
                    throw e;
                }
            }
        }

        abstract protected void doSchedule();
        abstract protected void doCancel();
    }
}