--liquibase formatted sql
--changeset tms:0004-identity-outbox
--comment: Outbox table consumed by platform-lib-messaging dispatcher.

CREATE TABLE identity.outbox_event (
    id              UUID PRIMARY KEY,
    version         BIGINT NOT NULL DEFAULT 0,
    tenant_id       UUID,
    topic           VARCHAR(200) NOT NULL,
    message_key     VARCHAR(200) NOT NULL,
    payload         JSONB NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    sent_at         TIMESTAMPTZ,
    attempts        INTEGER NOT NULL DEFAULT 0
);
CREATE INDEX ix_outbox_unsent ON identity.outbox_event (sent_at NULLS FIRST, id) WHERE sent_at IS NULL;
