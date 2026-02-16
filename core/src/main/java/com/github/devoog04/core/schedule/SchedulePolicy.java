package com.github.devoog04.core.schedule;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;

/**
 * 스케줄링 정책을 생성하는 정적 팩토리 메서드를 제공합니다.
 */
@Slf4j
@Getter
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class SchedulePolicy {
    private final ScheduleType type;
    private final String expression;
    private final Duration interval;

    /**
     * Cron 표현식을 기반으로 하는 정책을 생성합니다.
     * @param expression 크론 표현식 (예: "0 0/5 * * * ?")
     */
    public static SchedulePolicy cron(String expression) {
        validateCron(expression);
        return new SchedulePolicy(ScheduleType.CRON, expression, null);
    }

    /**
     * 이전 작업이 완료된 시점부터 일정 시간 대기 후 실행하는 정책을 생성합니다.
     * @param interval 대기 시간 (Duration)
     */
    public static SchedulePolicy fixedDelay(Duration interval) {
        validateInterval(interval);
        return new SchedulePolicy(ScheduleType.FIXED_DELAY, null, interval);
    }

    /**
     * 작업의 시작 시점을 기준으로 일정한 간격마다 실행하는 정책을 생성합니다.
     * @param interval 실행 간격 (Duration)
     */
    public static SchedulePolicy fixedRate(Duration interval) {
        validateInterval(interval);
        return new SchedulePolicy(ScheduleType.FIXED_RATE, null, interval);
    }

    /**
     * Cron 표현식의 유효성을 검사합니다.
     */
    private static void validateCron(String expression) {
        if (expression == null || expression.trim().isEmpty()) {
            throw new IllegalArgumentException("An expression is required.");
        }
    }

    /**
     * interval의 유효성을 검사합니다.
     */
    private static void validateInterval(Duration interval) {
        if (interval == null) {
            throw new IllegalArgumentException("interval is required.");
        }
        if (interval.isNegative() || interval.isZero()) {
            throw new IllegalArgumentException("interval must be positive.");
        }
    }
}