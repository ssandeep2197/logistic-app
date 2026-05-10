package com.handa.tms.platform.messaging.outbox;

import com.handa.tms.platform.core.outbox.OutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {

    /**
     * Picks unsent rows for the dispatcher, FOR UPDATE SKIP LOCKED so multiple
     * dispatcher replicas can claim disjoint batches without blocking each other.
     */
    @Query(value = """
            SELECT * FROM outbox_event
            WHERE sent_at IS NULL AND attempts < :maxAttempts
            ORDER BY id
            LIMIT :batchSize
            FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    List<OutboxEvent> claimBatch(@Param("batchSize") int batchSize, @Param("maxAttempts") int maxAttempts);
}
