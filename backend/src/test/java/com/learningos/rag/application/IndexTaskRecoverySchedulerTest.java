package com.learningos.rag.application;

import com.learningos.config.IndexRecoveryProperties;
import com.learningos.config.IndexWorkerProperties;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class IndexTaskRecoverySchedulerTest {

    private static final Clock FIXED_CLOCK = Clock.fixed(
            Instant.parse("2026-06-06T08:00:00Z"),
            ZoneOffset.UTC
    );

    @Test
    void recoverOnStartupCallsIndexServiceWithCurrentTimeForLeaseExpiry() {
        IndexService indexService = mock(IndexService.class);
        IndexRecoveryProperties properties = new IndexRecoveryProperties(
                true,
                true,
                Duration.ofMinutes(15),
                Duration.ofMinutes(5)
        );
        IndexTaskRecoveryScheduler scheduler = new IndexTaskRecoveryScheduler(
                indexService,
                properties,
                workerProperties(),
                FIXED_CLOCK
        );

        scheduler.recoverOnStartup();

        verify(indexService).recoverTimedOutRunningTasks(
                Instant.parse("2026-06-06T08:00:00Z"),
                2,
                Duration.ofSeconds(30)
        );
    }

    @Test
    void recoverOnScheduleCallsIndexServiceOnceWithCurrentTimeForLeaseExpiry() {
        IndexService indexService = mock(IndexService.class);
        IndexRecoveryProperties properties = new IndexRecoveryProperties(
                true,
                true,
                Duration.ofMinutes(30),
                Duration.ofMinutes(5)
        );
        IndexTaskRecoveryScheduler scheduler = new IndexTaskRecoveryScheduler(
                indexService,
                properties,
                workerProperties(),
                FIXED_CLOCK
        );

        scheduler.recoverOnSchedule();

        verify(indexService).recoverTimedOutRunningTasks(
                Instant.parse("2026-06-06T08:00:00Z"),
                2,
                Duration.ofSeconds(30)
        );
    }

    @Test
    void disabledRecoverySkipsStartupAndScheduledCalls() {
        IndexService indexService = mock(IndexService.class);
        IndexRecoveryProperties properties = new IndexRecoveryProperties(
                false,
                true,
                Duration.ofMinutes(30),
                Duration.ofMinutes(5)
        );
        IndexTaskRecoveryScheduler scheduler = new IndexTaskRecoveryScheduler(
                indexService,
                properties,
                workerProperties(),
                FIXED_CLOCK
        );

        scheduler.recoverOnStartup();
        scheduler.recoverOnSchedule();

        verifyNoInteractions(indexService);
    }

    @Test
    void startupRecoveryCanBeDisabledWithoutDisablingScheduledRecovery() {
        IndexService indexService = mock(IndexService.class);
        IndexRecoveryProperties properties = new IndexRecoveryProperties(
                true,
                false,
                Duration.ofMinutes(30),
                Duration.ofMinutes(5)
        );
        IndexTaskRecoveryScheduler scheduler = new IndexTaskRecoveryScheduler(
                indexService,
                properties,
                workerProperties(),
                FIXED_CLOCK
        );

        scheduler.recoverOnStartup();

        verifyNoInteractions(indexService);
    }

    private IndexWorkerProperties workerProperties() {
        return new IndexWorkerProperties(
                true,
                Duration.ofSeconds(5),
                2,
                Duration.ofMinutes(2),
                Duration.ofSeconds(30),
                2
        );
    }
}
