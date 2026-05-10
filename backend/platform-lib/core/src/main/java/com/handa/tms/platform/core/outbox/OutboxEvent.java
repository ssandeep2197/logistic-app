package com.handa.tms.platform.core.outbox;

import com.handa.tms.platform.core.persistence.BaseEntity;
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

    /** JSON payload — keep schema-versioned in {@code backend/contracts}. */
    @Column(name = "payload", nullable = false, columnDefinition = "jsonb")
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
