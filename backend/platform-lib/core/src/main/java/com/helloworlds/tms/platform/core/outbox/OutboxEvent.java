package com.helloworlds.tms.platform.core.outbox;

import com.helloworlds.tms.platform.core.persistence.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * Outbox row, written in the same DB transaction that mutates business state.
 * A separate dispatcher (in {@code platform-lib-messaging}) reads new rows and
 * publishes them to Kafka with at-least-once delivery, then marks them sent.
 * <p>
 * This is what makes "user signs up" and "user.created event reaches every
 * other service" reliable across a service crash — the event is committed
 * with the user row or not at all.
 */
@Entity
@Table(name = "outbox_event",
       indexes = @Index(name = "ix_outbox_unsent", columnList = "sent_at,id"))
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OutboxEvent extends BaseEntity {

    @Column(name = "tenant_id", columnDefinition = "uuid")
    private UUID tenantId;

    /** Kafka topic name, e.g. {@code "user.created.v1"}. */
    @Column(name = "topic", nullable = false, length = 200)
    private String topic;

    /** Kafka message key — usually the aggregate id, gives us per-aggregate ordering. */
    @Column(name = "message_key", nullable = false, length = 200)
    private String messageKey;

    /**
     * JSON payload as a string — keep schema-versioned in {@code backend/contracts}.
     * Stored as TEXT (not JSONB) because the dispatcher reads the row whole and
     * ships it to Kafka unchanged; we never query into it.  If a future feature
     * needs JSON-path queries, switch to {@code @JdbcTypeCode(SqlTypes.JSON)}
     * with a typed payload object instead of {@code String}.
     */
    @Column(name = "payload", nullable = false, columnDefinition = "text")
    private String payload;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /** Null until the dispatcher confirms Kafka acked the publish. */
    @Column(name = "sent_at")
    private Instant sentAt;

    @Column(name = "attempts", nullable = false)
    private int attempts;

    @PrePersist
    void onPersist() {
        if (createdAt == null) createdAt = Instant.now();
    }

    public static OutboxEvent of(String topic, String key, String payload, UUID tenantId) {
        OutboxEvent e = new OutboxEvent();
        e.setId(UUID.randomUUID());
        e.tenantId = tenantId;
        e.topic = topic;
        e.messageKey = key;
        e.payload = payload;
        return e;
    }
}
