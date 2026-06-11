package com.learningos.rag.application;

import com.learningos.config.IndexWorkerProperties;
import com.learningos.rag.domain.KbIndexTask;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

@Component
public class IndexTaskWorkerScheduler {

    private static final Logger log = LoggerFactory.getLogger(IndexTaskWorkerScheduler.class);

    private final IndexService indexService;
    private final IndexWorkerProperties properties;
    private final Clock clock;
    private final String workerId;

    @Autowired
    public IndexTaskWorkerScheduler(
            IndexService indexService,
            IndexWorkerProperties properties
    ) {
        this(indexService, properties, Clock.systemUTC(), workerId());
    }

    IndexTaskWorkerScheduler(
            IndexService indexService,
            IndexWorkerProperties properties,
            Clock clock,
            String workerId
    ) {
        this.indexService = indexService;
        this.properties = properties;
        this.clock = clock;
        this.workerId = workerId;
    }

    @Scheduled(fixedDelayString = "${learning-os.rag.index-worker.fixed-delay:5s}")
    public void processDueTasksOnSchedule() {
        runOnce();
    }

    int runOnce() {
        if (!properties.enabled()) {
            return 0;
        }
        Instant now = Instant.now(clock);
        var claimedTasks = indexService.claimDuePendingTasks(
                now,
                properties.batchSize(),
                workerId,
                properties.leaseDuration()
        );
        for (KbIndexTask task : claimedTasks) {
            try {
                indexService.processIndexTask(
                        task.getId(),
                        workerId,
                        properties.maxRetryCount(),
                        properties.retryBackoff(),
                        properties.leaseDuration()
                );
            } catch (RuntimeException exception) {
                log.warn("RAG index worker skipped task after processing failure: taskId={}", task.getId(), exception);
            }
        }
        return claimedTasks.size();
    }

    private static String workerId() {
        return "index-worker-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }
}
