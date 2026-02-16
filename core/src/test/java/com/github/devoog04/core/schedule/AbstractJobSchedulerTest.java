package com.github.devoog04.core.schedule;

import com.github.devoog04.core.definition.JobDefinition;
import com.github.devoog04.core.definition.JobDefinitionKey;
import com.github.devoog04.core.schedule.exception.DuplicateScheduleException;
import com.github.devoog04.core.schedule.exception.NotFoundScheduleException;
import com.github.devoog04.core.schedule.exception.ScheduleExecutionException;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("AbstractJobScheduler 검증")
@ExtendWith(MockitoExtension.class)
class AbstractJobSchedulerTest {
    private AbstractJobScheduler scheduler;

    private static JobDefinitionKey key;
    private static JobDefinition definition;
    private static SchedulePolicy policy;

    @BeforeAll
    static void beforeAll() {
        key = mock(JobDefinitionKey.class);

        definition = mock(JobDefinition.class);
        when(definition.getKey()).thenReturn(key);

        policy = mock(SchedulePolicy.class);
    }

    @BeforeEach
    void setUp() {
        scheduler = mock(AbstractJobScheduler.class, withSettings().useConstructor().defaultAnswer(CALLS_REAL_METHODS));
    }

    @AfterEach
    void tearDown() {
        scheduler.container.clear();
    }

    @Nested
    @DisplayName("schedule 메서드 검증")
    class Context_Schedule {
        @Test
        @DisplayName("새로운 job 등록할 때, Success")
        void givenNewJob_whenSchedule_thenSuccess() {
            // Given: 신규 manager 생성
            AbstractJobScheduler.ScheduleManager newManager = mock(AbstractJobScheduler.ScheduleManager.class);
            when(scheduler.createManager(definition, policy)).thenReturn(newManager);

            // When: 로직 실행
            scheduler.schedule(definition, policy);

            // Then: 신규 manager 생성 및 스케줄 호출
            verify(scheduler, times(1)).createManager(definition, policy);
            verify(newManager, times(1)).schedule();
            // Then: 신규 manager 등록 확인 
            assertThat(scheduler.container).containsKey(key).containsValue(newManager);
        }

        @Test
        @DisplayName("기존 job이 존재할 때, 기존 job 유지 및 Exception 전파")
        void givenExistingJob_whenSchedule_thenKeepJobAndExceptionPropagation() {
            // Given: 기존 manager 등록
            AbstractJobScheduler.ScheduleManager oldManager = mock(AbstractJobScheduler.ScheduleManager.class);
            JobDefinitionKey key = definition.getKey();
            scheduler.container.put(key, oldManager);

            // When & Then: 로직 실행 & 예외 발생
            assertThatThrownBy(() -> scheduler.schedule(definition, policy))
                    .isInstanceOf(DuplicateScheduleException.class);

            // Then: 신규 manager 생성 호출 X
            verify(scheduler, never()).createManager(any(), any());
            // Then: 기존 manager 등록 확인
            assertThat(scheduler.container).containsKey(key).containsValue(oldManager);
        }

        @Test
        @DisplayName("신규 manager 생성 실패할 때, 기존 job 유지 및 Exception 전파")
        void givenNewManagerCreationError_whenSchedule_thenKeepJobAndExceptionPropagation() {
            // Given: 신규 manager 생성 -> Exception
            when(scheduler.createManager(definition, policy)).thenThrow(new RuntimeException("Test error message"));

            // When & Then: 로직 실행 & 예외 발생
            assertThatThrownBy(() -> scheduler.schedule(definition, policy))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Test error message");

            // Then: 신규 manager 생성 호출
            verify(scheduler, times(1)).createManager(definition, policy);
            // Then : manager 등록 안됨 확인
            assertThat(scheduler.container.get(key)).isNull();
        }

        @Test
        @DisplayName("신규 manager 스케줄 실패할 때, 신규 job 등록 X 및 Exception 전파")
        void givenNewManagerScheduleError_whenSchedule_thenUnregisteredNewJobAndExceptionPropagation() {
            // Given: 신규 manager 생성
            AbstractJobScheduler.ScheduleManager newManager = mock(AbstractJobScheduler.ScheduleManager.class);
            when(scheduler.createManager(definition, policy)).thenReturn(newManager);
            // Given: 신규 manager 스케줄 -> Exception
            doThrow(new RuntimeException("Test error message")).when(newManager).schedule();

            // When & Then: 로직 실행 & 예외 발생
            assertThatThrownBy(() -> scheduler.schedule(definition, policy))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Test error message");

            // Then: 신규 manager 생성 호출
            verify(scheduler, times(1)).createManager(definition, policy);
            // Then : manager 등록 안됨 확인
            assertThat(scheduler.container.get(key)).isNull();
        }
    }

    @Nested
    @DisplayName("reschedule 메서드 검증")
    class Context_Reschedule {

        @Test
        @DisplayName("기존 job 있을 때, Success")
        void givenExistingJob_whenReschedule_thenSuccess() {
            // Given: 기존 manager 등록
            AbstractJobScheduler.ScheduleManager oldManager = mock(AbstractJobScheduler.ScheduleManager.class);
            when(oldManager.getJobDefinition()).thenReturn(definition);
            scheduler.container.put(key, oldManager);

            // Given : 신규 policy 생성
            SchedulePolicy newPolicy = mock(SchedulePolicy.class);

            // Given: 신규 manager 생성
            AbstractJobScheduler.ScheduleManager newManager = mock(AbstractJobScheduler.ScheduleManager.class);
            when(scheduler.createManager(definition, newPolicy)).thenReturn(newManager);

            // When : 로직 실행
            scheduler.reschedule(key, newPolicy);

            // Then: 실행 순서 검증
            InOrder inOrder = inOrder(scheduler, oldManager, newManager);
            // Then : 기존 manager 취소 호출
            inOrder.verify(oldManager, times(1)).cancel();
            // Then : 신규 manager 생성 호출
            inOrder.verify(scheduler, times(1)).createManager(definition, newPolicy);
            // Then : 신규 manager 스케줄 호출
            inOrder.verify(newManager, times(1)).schedule();
            
            // Then : 신규 manager 등록 확인
            assertThat(scheduler.container).hasSize(1);
            assertThat(scheduler.container.get(key)).isEqualTo(newManager);
        }

        @Test
        @DisplayName("기존 job 없을 때, 변화 없음 및 Exception 전파")
        void givenNotExistingJob_whenReschedule_thenIgnoreAndExceptionPropagation() {
            // When & Then: 로직 실행 & 예외 발생
            assertThatThrownBy(() -> scheduler.reschedule(key, policy))
                    .isInstanceOf(NotFoundScheduleException.class);
            
            // Then : 신규 manager 생성 호출 X
            verify(scheduler, never()).createManager(any(), any());
            // Then : manager 등록 안됨 확인
            assertThat(scheduler.container.get(key)).isNull();
        }

        @Test
        @DisplayName("기존 manager 취소 실패할 때, 변화 없음(기존 manager 유지) 및 Exception 전파")
        void givenOldManagerCancelError_whenReschedule_thenIgnoreAndExceptionPropagation() {
            // Given : 기존 manager 등록
            AbstractJobScheduler.ScheduleManager oldManager = mock(AbstractJobScheduler.ScheduleManager.class);
            scheduler.container.put(key, oldManager);
            
            // Given : 기존 manager 취소 -> Exception
            doThrow(new RuntimeException("Test error message")).when(oldManager).cancel();
            
            // Given : 교체할 policy 생성
            SchedulePolicy newPolicy = mock(SchedulePolicy.class);

            // When & Then: 로직 실행 & 예외 발생
            assertThatThrownBy(() -> scheduler.reschedule(key, newPolicy))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Test error message");

            // Then : 기존 manager 취소 호출
            verify(oldManager, times(1)).cancel();
            // Then : 기존 manager 등록 확인
            assertThat(scheduler.container.get(key)).isEqualTo(oldManager);
        }

        @Test
        @DisplayName("신규 manager 생성 실패할 때, 기존 manager 롤백 및 Exception 전파")
        void givenNewManagerCreationError_whenReschedule_thenOldManagerRollbackAndExceptionPropagation() {
            // Given : 기존 manager 등록
            AbstractJobScheduler.ScheduleManager oldManager = mock(AbstractJobScheduler.ScheduleManager.class);
            when(oldManager.getJobDefinition()).thenReturn(definition);
            scheduler.container.put(key, oldManager);
            
            // Given : 교체할 policy 생성
            SchedulePolicy newPolicy = mock(SchedulePolicy.class);

            // Given : 신규 manager 생성 -> Exception
            when(scheduler.createManager(definition, newPolicy)).thenThrow(new RuntimeException("Test error message"));

            // When & Then: 로직 실행 & 예외 발생
            assertThatThrownBy(() -> scheduler.reschedule(key, newPolicy))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Test error message");

            // Then: 실행 순서 검증
            InOrder inOrder = inOrder(scheduler, oldManager);
            // Then : 기존 manager 취소 호출
            inOrder.verify(oldManager, times(1)).cancel();
            // Then : 신규 manager 생성 호출
            inOrder.verify(scheduler, times(1)).createManager(definition, newPolicy);
            // Then : 기존 manager 스케줄 호출
            inOrder.verify(oldManager, times(1)).schedule();

            // Then : 기존 manager 등록 확인
            assertThat(scheduler.container.get(key)).isEqualTo(oldManager);
        }

        @Test
        @DisplayName("신규 manager 스케줄 실패할 때, 기존 manager 롤백 및 Exception 전파")
        void givenNewManagerScheduleError_whenReschedule_thenOldManagerRollbackAndExceptionPropagation() {
            // Given : 기존 manager 등록
            AbstractJobScheduler.ScheduleManager oldManager = mock(AbstractJobScheduler.ScheduleManager.class);
            when(oldManager.getJobDefinition()).thenReturn(definition);
            scheduler.container.put(key, oldManager);

            // Given : 교체할 policy 생성
            SchedulePolicy newPolicy = mock(SchedulePolicy.class);

            // Given : 신규 manager 생성
            AbstractJobScheduler.ScheduleManager newManager = mock(AbstractJobScheduler.ScheduleManager.class);
            when(scheduler.createManager(definition, newPolicy)).thenReturn(newManager);

            // Given : 신규 manager 스케줄 -> Exception
            doThrow(new RuntimeException("Test error message")).when(newManager).schedule();

            // When & Then: 로직 실행 & 예외 발생
            assertThatThrownBy(() -> scheduler.reschedule(key, newPolicy))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Test error message");

            // Then: 실행 순서 검증
            InOrder inOrder = inOrder(scheduler, oldManager, newManager);
            // Then : 기존 manager 취소 호출
            inOrder.verify(oldManager, times(1)).cancel();
            // Then : 신규 manager 생성 호출
            inOrder.verify(scheduler, times(1)).createManager(definition, newPolicy);
            // Then : 신규 manager 스케줄 호출
            inOrder.verify(newManager, times(1)).schedule();
            // Then : 기존 manager 스케줄 호출
            inOrder.verify(oldManager, times(1)).schedule();

            // Then : 기존 manager 등록 확인
            assertThat(scheduler.container.get(key)).isEqualTo(oldManager);
        }

        @Test
        @DisplayName("신규 manager 스케줄 실패 이후 기존 manager 롤백 실패할 때, job 제거 및 Exception 전파")
        void givenNewManagerScheduleErrorAndOldManagerRollbackError_whenReschedule_thenDeleteJobAndExceptionPropagation() {
            // Given : 기존 manager 등록
            AbstractJobScheduler.ScheduleManager oldManager = mock(AbstractJobScheduler.ScheduleManager.class);
            when(oldManager.getJobDefinition()).thenReturn(definition);
            scheduler.container.put(key, oldManager);

            // Given : 기존 manager 스케줄 -> Exception
            doThrow(new RuntimeException("Test error message1")).when(oldManager).schedule();

            // Given : 교체할 policy 생성
            SchedulePolicy newPolicy = mock(SchedulePolicy.class);

            // Given : 신규 manager 생성
            AbstractJobScheduler.ScheduleManager newManager = mock(AbstractJobScheduler.ScheduleManager.class);
            when(scheduler.createManager(definition, newPolicy)).thenReturn(newManager);

            // Given : 신규 manager 스케줄 -> Exception
            doThrow(new RuntimeException("Test error message2")).when(newManager).schedule();

            // When & Then: 로직 실행 & 예외 발생
            assertThatThrownBy(() -> scheduler.reschedule(key, newPolicy))
                    .isInstanceOf(ScheduleExecutionException.class);

            // Then: 실행 순서 검증
            InOrder inOrder = inOrder(scheduler, oldManager, newManager);
            // Then : 기존 manager 취소 호출
            inOrder.verify(oldManager, times(1)).cancel();
            // Then : 신규 manager 생성 호출
            inOrder.verify(scheduler, times(1)).createManager(definition, newPolicy);
            // Then : 신규 manager 스케줄 호출
            inOrder.verify(newManager, times(1)).schedule();
            // Then : 기존 manager 스케줄 호출
            inOrder.verify(oldManager, times(1)).schedule();

            // Then : 기존/신규 manager 미등록 확인
            assertThat(scheduler.container.get(key)).isNull();
        }
    }

    @Nested
    @DisplayName("unschedule 메서드 검증")
    class Context_Unschedule {

        @Test
        @DisplayName("기존 key가 있을 때, Success")
        void givenExistingKey_whenUnschedule_thenSuccess() {
            // Given: 기존 manager 등록
            AbstractJobScheduler.ScheduleManager oldManager = mock(AbstractJobScheduler.ScheduleManager.class);
            scheduler.container.put(key, oldManager);

            // When : 로직 실행
            scheduler.unschedule(key);

            // Then : 기존 manager 취소 호출
            verify(oldManager, times(1)).cancel();
            // Then : 기존 manager 미등록 확인
            assertThat(scheduler.container).doesNotContainKey(key);
        }

        @Test
        @DisplayName("기존 job 없을 때, 진행 X")
        void givenNotExistingKey_whenUnschedule_thenIgnore() {
            // When : 로직 실행
            scheduler.unschedule(key);

            // Then : 기존 manager 미등록 확인
            assertThat(scheduler.container).doesNotContainKey(key);
        }

        @Test
        @DisplayName("기존 manager 취소 실패할 때, 기존 job 유지 및 Exception 전파")
        void givenCancelError_whenUnschedule_thenKeepJobAndExceptionPropagation() {
            // Given: 기존 manager 등록
            AbstractJobScheduler.ScheduleManager oldManager = mock(AbstractJobScheduler.ScheduleManager.class);
            scheduler.container.put(key, oldManager);

            // Given : 기존 manager 취소 -> Exception
            doThrow(new RuntimeException("Test error message")).when(oldManager).cancel();

            // When & Then: 로직 실행 & 예외 발생
            assertThatThrownBy(() -> scheduler.unschedule(key) )
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Test error message");


            // Then : 기존 manager 취소 호출
            verify(oldManager, times(1)).cancel();
            // Then : 기존 manager 등록 확인
            assertThat(scheduler.container).containsKey(key);
            assertThat(scheduler.container.get(key)).isEqualTo(oldManager);
        }
    }
}