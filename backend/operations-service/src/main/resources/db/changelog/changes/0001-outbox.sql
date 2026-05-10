--liquibase formatted sql
--changeset tms:operations-0001-outbox
CREATE TABLE operations.outbox_event (
    id              UUID PRIMARY KEY,
    version         BIGINT NOT NULL DEFAULT 0,
    tenant_id       UUID,
    topic           VARCHAR(200) NOT NULL,
    message_key     VARCHAR(200) NOT NULL,
    payload         TEXT NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    sent_at         TIMESTAMPTZ,
    attempts        INTEGER NOT NULL DEFAULT 0
);
CREATE INDEX ix_operations_outbox_unsent ON operations.outbox_event (sent_at NULLS FIRST, id) WHERE sent_at IS NULL;
