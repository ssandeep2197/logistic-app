--liquibase formatted sql
--changeset tms:operations-0002-core
--comment: Customer + Load.  Single pickup, single delivery — multi-stop loads
--         get their own table later when a customer asks.  Rate stored in
--         cents (BIGINT) to dodge floating-point pain; the app uses
--         BigDecimal at the boundary.

CREATE TABLE operations.customer (
    id              UUID PRIMARY KEY,
    version         BIGINT NOT NULL DEFAULT 0,
    tenant_id       UUID NOT NULL,
    name            VARCHAR(200) NOT NULL,
    contact_name    VARCHAR(200),
    contact_email   VARCHAR(320),
    contact_phone   VARCHAR(40),
    billing_address TEXT,
    notes           TEXT,
    status          VARCHAR(20) NOT NULL DEFAULT 'active',
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (tenant_id, name)
);
CREATE INDEX ix_customer_tenant ON operations.customer(tenant_id);
CREATE INDEX ix_customer_status ON operations.customer(tenant_id, status);

CREATE TABLE operations.load (
    id                       UUID PRIMARY KEY,
    version                  BIGINT NOT NULL DEFAULT 0,
    tenant_id                UUID NOT NULL,
    customer_id              UUID NOT NULL REFERENCES operations.customer(id) ON DELETE RESTRICT,
    reference_number         VARCHAR(40) NOT NULL,                 -- carrier's internal load #, e.g. "L-2026-001"
    -- planned | in_transit | delivered | cancelled
    status                   VARCHAR(20) NOT NULL DEFAULT 'planned',
    rate_cents               BIGINT,                               -- nullable until rate confirmed
    rate_currency            CHAR(3) NOT NULL DEFAULT 'USD',
    pickup_location          TEXT NOT NULL,
    pickup_window_start      TIMESTAMPTZ,
    pickup_window_end        TIMESTAMPTZ,
    delivery_location        TEXT NOT NULL,
    delivery_window_start    TIMESTAMPTZ,
    delivery_window_end      TIMESTAMPTZ,
    assigned_driver_name     VARCHAR(200),                         -- free text v1; real driver entity later
    notes                    TEXT,
    delivered_at             TIMESTAMPTZ,                          -- set when status → delivered
    cancelled_at             TIMESTAMPTZ,                          -- set when status → cancelled
    created_at               TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at               TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (tenant_id, reference_number)
);
CREATE INDEX ix_load_tenant   ON operations.load(tenant_id);
CREATE INDEX ix_load_customer ON operations.load(customer_id);
CREATE INDEX ix_load_status   ON operations.load(tenant_id, status);
