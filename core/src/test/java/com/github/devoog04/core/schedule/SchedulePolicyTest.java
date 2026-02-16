package com.github.devoog04.core.schedule;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.function.Supplier;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

@DisplayName("SchedulePolicy 검증")
class SchedulePolicyTest {

    @Nested
    @DisplayName("cron 메서드 검증")
    class Context_Cron {
        @Test
        @DisplayName("expression이 글자가 있을 때 성공")
        void givenExistExpression_whenCreate_thenSuccess() {
            String expression = "aaa";
            assertSuccess(() -> SchedulePolicy.cron(expression), ScheduleType.CRON, expression, null);
        }

        @Test
        @DisplayName("expression이 null or 공백일 때 Exception")
        void givenNotExistExpression_whenCreate_thenThrowsException() {
            assertFailure( () -> SchedulePolicy.cron(null), "An expression is required.");
            assertFailure( () -> SchedulePolicy.cron(""), "An expression is required.");
            assertFailure( () -> SchedulePolicy.cron(" "), "An expression is required.");
            assertFailure( () -> SchedulePolicy.cron("  "), "An expression is required.");
        }
    }


    @Nested
    @DisplayName("fixedDelay 메서드 검증")
    class Context_FixedDelay {
        @Test
        @DisplayName("interval이 양수일 때 성공")
        void givenPositiveInterval_whenCreate_thenSuccess() {
            Duration interval = Duration.ofSeconds(1);
            assertSuccess(() -> SchedulePolicy.fixedDelay(interval), ScheduleType.FIXED_DELAY, null, interval);
        }

        @Test
        @DisplayName("interval이 null or 0 or 음수일 때 Exception")
        void givenInvalidInterval_whenCreate_thenThrowsException() {
            assertFailure( () -> SchedulePolicy.fixedDelay(null), "interval is required.");
            assertFailure( () -> SchedulePolicy.fixedDelay(Duration.ZERO), "interval must be positive.");
            assertFailure( () -> SchedulePolicy.fixedDelay(Duration.ofSeconds(-1)), "interval must be positive.");
        }
    }

    @Nested
    @DisplayName("fixedRate 메서드 검증")
    class Context_FixedRate {
        @Test
        @DisplayName("interval이 양수일 때 성공")
        void givenPositiveInterval_whenCreate_thenSuccess() {
            Duration interval = Duration.ofSeconds(1);
            assertSuccess(() -> SchedulePolicy.fixedRate(interval), ScheduleType.FIXED_RATE, null, interval);
        }

        @Test
        @DisplayName("interval이 null or 0 or 음수일 때 Exception")
        void givenInvalidInterval_whenCreate_thenThrowsException() {
            assertFailure( () -> SchedulePolicy.fixedRate(null), "interval is required.");
            assertFailure( () -> SchedulePolicy.fixedRate(Duration.ZERO), "interval must be positive.");
            assertFailure( () -> SchedulePolicy.fixedRate(Duration.ofSeconds(-1)), "interval must be positive.");
        }
    }

    // --- 공통 검증 로직 ---
    private void assertSuccess(Supplier<SchedulePolicy> supplier, ScheduleType type, String expression, Duration interval) {
        SchedulePolicy policy = supplier.get();

        assertThat(policy).isNotNull();
        assertThat(policy.getType()).isEqualTo(type);
        assertThat(policy.getExpression()).isEqualTo(expression);
        assertThat(policy.getInterval()).isEqualTo(interval);
    }

    private void assertFailure(Supplier<SchedulePolicy> supplier, String message) {
        assertThatThrownBy(supplier::get)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(message);
    }
}