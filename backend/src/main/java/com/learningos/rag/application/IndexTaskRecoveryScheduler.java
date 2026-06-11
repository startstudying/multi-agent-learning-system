package com.learningos.rag.application;

import com.learningos.config.IndexRecoveryProperties;
import com.learningos.config.IndexWorkerProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;

@Component
public class IndexTaskRecoveryScheduler {

    private final IndexService indexService;
    private final IndexRecoveryProperties properties;
    private final IndexWorkerProperties workerProperties;
    private final Clock clock;

    @Autowired
    public IndexTaskRecoveryScheduler(
            IndexService indexService,
            IndexRecoveryProperties properties,
            IndexWorkerProperties workerProperties
    ) {
        this(indexService, properties, workerProperties, Clock.systemUTC());
    }

    IndexTaskRecoveryScheduler(
            IndexService indexService,
            IndexRecoveryProperties properties,
            IndexWorkerProperties workerProperties,
            Clock clock
    ) {
        this.indexService = indexService;
        this.properties = properties;
        this.workerProperties = workerProperties;
        this.clock = clock;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void recoverOnStartup() {
        if (!properties.enabled() || !properties.runOnStartup()) {
            return;
        }
        recoverOnce();
    }

    @Scheduled(fixedDelayString = "${learning-os.rag.index-recovery.fixed-delay:5m}")
    public void recoverOnSchedule() {
        if (!properties.enabled()) {
            return;
        }
        recoverOnce();
    }

    private void recoverOnce() {
        Instant timeoutCutoff = Instant.now(clock);
        indexService.recoverTimedOutRunningTasks(
                timeoutCutoff,
                workerProperties.maxRetryCount(),
                workerProperties.retryBackoff()
        );
    }
}
