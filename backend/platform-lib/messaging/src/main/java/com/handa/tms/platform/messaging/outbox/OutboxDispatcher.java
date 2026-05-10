package com.handa.tms.platform.messaging.outbox;

import com.handa.tms.platform.core.outbox.OutboxEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Polls the outbox table every second and publishes new rows to Kafka.  Each
 * row is published with its key (aggregate id) so consumers see consistent
 * per-aggregate ordering.  After Kafka acks, we mark {@code sent_at} in the
 * same transaction that claimed the row.
 * <p>
 * Why poll instead of CDC: simpler ops on a self-hosted VPS (no Debezium
 * connector to run); 1-second latency is fine for our event types.
 */
public class OutboxDispatcher {

    private static final Logger log = LoggerFactory.getLogger(OutboxDispatcher.class);
    private static final int BATCH_SIZE = 100;
    private static final int MAX_ATTEMPTS = 10;

    private final OutboxEventRepository repo;
    private final KafkaTemplate<String, String> kafka;

    public OutboxDispatcher(OutboxEventRepository repo, KafkaTemplate<String, String> kafka) {
        this.repo = repo;
        this.kafka = kafka;
    }

    @Scheduled(fixedDelayString = "${tms.messaging.outbox.poll-ms:1000}")
    @Transactional
    public void dispatch() {
        List<OutboxEvent> batch = repo.claimBatch(BATCH_SIZE, MAX_ATTEMPTS);
        if (batch.isEmpty()) return;

        for (OutboxEvent ev : batch) {
            try {
                kafka.send(ev.getTopic(), ev.getMessageKey(), ev.getPayload())
                        .get(5, TimeUnit.SECONDS);
                ev.setSentAt(Instant.now());
            } catch (InterruptedException | ExecutionException | TimeoutException e) {
                log.warn("outbox publish failed topic={} key={} attempt={}",
                        ev.getTopic(), ev.getMessageKey(), ev.getAttempts() + 1, e);
                ev.setAttempts(ev.getAttempts() + 1);
            }
        }
        // JPA flushes on tx commit — sent_at and attempts persist together.
    }
}
