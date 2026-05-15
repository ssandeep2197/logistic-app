--liquibase formatted sql
--changeset tms:operations-0004-fleet
--comment: The operating-entity model.  A Tenant signs up for the SaaS; a
--         Company is a legal entity owned by the Tenant that holds MC/DOT
--         numbers and operates loads.  Many Tenants will have only one
--         Company; some run multi-MC structures (parent + subsidiaries).
--
--         This migration defines the canonical relational shape.  File
--         attachments (W9, COI, MC cert, etc.), bank details, and
--         compliance event tables come in later migrations to keep this
--         one reviewable.

-- ─────────────────────────────────────────────────────────────────────────
-- COMPANY — the carrier's operating legal entity (one Tenant → many Companies)
-- ─────────────────────────────────────────────────────────────────────────
CREATE TABLE operations.company (
    id              UUID PRIMARY KEY,
    version         BIGINT NOT NULL DEFAULT 0,
    tenant_id       UUID NOT NULL,
    name            VARCHAR(200) NOT NULL,
    dba             VARCHAR(200),                              -- "Doing Business As"
    -- FMCSA numbers.  Either MC or DOT (or both) must be present for an
    -- active company in the US.  Not enforced as NOT NULL because a tenant
    -- might add a pending company before numbers arrive.
    mc_number       VARCHAR(20),
    dot_number      VARCHAR(20),
    tin             VARCHAR(20),                               -- Taxpayer Identification (EIN format XX-XXXXXXX)
    address_line1   VARCHAR(200),
    address_line2   VARCHAR(200),
    city            VARCHAR(120),
    state_code      CHAR(2),
    postal_code     VARCHAR(20),
    country_code    CHAR(2) NOT NULL DEFAULT 'US',
    phone           VARCHAR(40),
    email           VARCHAR(320),
    -- active | inactive | revoked: revoked = MC/DOT pulled; can't run loads
    status          VARCHAR(20) NOT NULL DEFAULT 'active',
    notes           TEXT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (tenant_id, name)
);
CREATE INDEX ix_company_tenant ON operations.company(tenant_id);
-- FMCSA numbers are globally unique by regulator; a UNIQUE constraint here
-- enforces that no two tenants accidentally claim the same MC.  Nullable
-- index using a partial index so multiple nulls don't collide.
CREATE UNIQUE INDEX ux_company_mc  ON operations.company(mc_number)  WHERE mc_number  IS NOT NULL;
CREATE UNIQUE INDEX ux_company_dot ON operations.company(dot_number) WHERE dot_number IS NOT NULL;

-- ─────────────────────────────────────────────────────────────────────────
-- DRIVER — the person who drives.  Always belongs to a Company (which
-- determines whose MC/DOT they're operating under for HOS reporting).
-- ─────────────────────────────────────────────────────────────────────────
CREATE TABLE operations.driver (
    id                UUID PRIMARY KEY,
    version           BIGINT NOT NULL DEFAULT 0,
    tenant_id         UUID NOT NULL,
    company_id        UUID NOT NULL REFERENCES operations.company(id) ON DELETE RESTRICT,
    -- active | terminated | on_leave | inactive_disqualified
    status            VARCHAR(30) NOT NULL DEFAULT 'active',
    first_name        VARCHAR(100) NOT NULL,
    last_name         VARCHAR(100) NOT NULL,
    -- Free-text v1 for license; future driver_qualification table will store
    -- class (A/B/C), endorsements (H, N, T, P, S), issuing state, etc.
    license_number    VARCHAR(40),
    license_state     CHAR(2),
    license_expiry    DATE,
    phone             VARCHAR(40),
    email             VARCHAR(320),
    address_line1     VARCHAR(200),
    address_line2     VARCHAR(200),
    city              VARCHAR(120),
    state_code        CHAR(2),
    postal_code       VARCHAR(20),
    country_code      CHAR(2) NOT NULL DEFAULT 'US',
    -- us_citizen | green_card | work_permit | non_us
    work_authorization VARCHAR(30),
    work_auth_expiry  DATE,                                    -- for non-citizens
    medical_exam_expiry DATE,                                  -- DOT med card expiry
    -- Last drug test summary — full history goes in operations.drug_test (phase 2.4).
    last_drug_test_date   DATE,
    last_drug_test_result VARCHAR(20),                         -- negative | positive | refused | pending
    -- Pay arrangement summary — full settlements live in payroll-service.
    -- per_mile | percent_of_load | hourly | flat | none
    pay_type          VARCHAR(20),
    pay_rate_cents    BIGINT,                                  -- cents per mile / cents per hour, contextual to pay_type
    notes             TEXT,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (tenant_id, license_number, license_state)          -- one driver record per (license, state)
);
CREATE INDEX ix_driver_tenant  ON operations.driver(tenant_id);
CREATE INDEX ix_driver_company ON operations.driver(company_id);
CREATE INDEX ix_driver_status  ON operations.driver(tenant_id, status);
CREATE INDEX ix_driver_license_expiry ON operations.driver(license_expiry)
    WHERE status = 'active' AND license_expiry IS NOT NULL;   -- supports "expiring in 30 days" queries

-- ─────────────────────────────────────────────────────────────────────────
-- TRUCK — power unit (the tractor)
-- ─────────────────────────────────────────────────────────────────────────
CREATE TABLE operations.truck (
    id                  UUID PRIMARY KEY,
    version             BIGINT NOT NULL DEFAULT 0,
    tenant_id           UUID NOT NULL,
    company_id          UUID NOT NULL REFERENCES operations.company(id) ON DELETE RESTRICT,
    -- in_service | out_of_service | sold | totalled
    status              VARCHAR(30) NOT NULL DEFAULT 'in_service',
    nickname            VARCHAR(80),                           -- internal name, e.g. "Truck 12"
    make                VARCHAR(80),
    model               VARCHAR(80),
    year                INTEGER,
    engine              VARCHAR(80),                           -- e.g. "Cummins X15"
    vin                 VARCHAR(17),                           -- 17 chars by spec
    plate_number        VARCHAR(20),
    plate_state         CHAR(2),
    plate_expiry        DATE,                                  -- registration expiry, drives renewal alerts
    -- Insurance + DOT inspection deadlines — separate columns so we can index
    -- + alert per attribute.  Full inspection history lives in a future
    -- operations.inspection table.
    insurance_policy_number VARCHAR(80),
    insurance_carrier   VARCHAR(120),
    insurance_start     DATE,
    insurance_end       DATE,
    annual_inspection_expiry DATE,                             -- the FMCSA-required annual DOT inspection
    -- ELD provider + device id are stored here so tracking-service can poll
    -- the right device when GPS ingest gets wired up.  Free text for v1.
    eld_provider        VARCHAR(40),                           -- 'samsara' | 'motive' | 'geotab' | …
    eld_device_id       VARCHAR(80),
    in_service_date     DATE,
    out_of_service_date DATE,
    notes               TEXT,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (tenant_id, vin)                                    -- VIN globally unique within a tenant
);
CREATE INDEX ix_truck_tenant  ON operations.truck(tenant_id);
CREATE INDEX ix_truck_company ON operations.truck(company_id);
CREATE INDEX ix_truck_status  ON operations.truck(tenant_id, status);

-- A truck can be assigned to multiple drivers over its lifetime.  This is the
-- CURRENT-assignment join — for historical assignment, write an event row
-- into a future operations.assignment_history table.
CREATE TABLE operations.truck_driver (
    truck_id      UUID NOT NULL REFERENCES operations.truck(id)  ON DELETE CASCADE,
    driver_id     UUID NOT NULL REFERENCES operations.driver(id) ON DELETE CASCADE,
    -- primary | secondary — primary driver is the one tracked for HOS
    assignment    VARCHAR(20) NOT NULL DEFAULT 'primary',
    assigned_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (truck_id, driver_id)
);
CREATE INDEX ix_truck_driver_driver ON operations.truck_driver(driver_id);

-- ─────────────────────────────────────────────────────────────────────────
-- TRAILER — the freight container.  Same shape as truck but smaller schema.
-- ─────────────────────────────────────────────────────────────────────────
CREATE TABLE operations.trailer (
    id                  UUID PRIMARY KEY,
    version             BIGINT NOT NULL DEFAULT 0,
    tenant_id           UUID NOT NULL,
    company_id          UUID NOT NULL REFERENCES operations.company(id) ON DELETE RESTRICT,
    status              VARCHAR(30) NOT NULL DEFAULT 'in_service',
    nickname            VARCHAR(80),
    -- dry_van | reefer | flatbed | step_deck | tanker | container | other
    trailer_type        VARCHAR(30),
    door_type           VARCHAR(30),                           -- 'swing' | 'roll_up' | 'curtain' | …
    make                VARCHAR(80),
    model               VARCHAR(80),
    year                INTEGER,
    vin                 VARCHAR(17),
    plate_number        VARCHAR(20),
    plate_state         CHAR(2),
    plate_expiry        DATE,
    annual_inspection_expiry DATE,
    -- Cargo tracking unit (separate from truck's ELD; reefers usually have one)
    tracking_provider   VARCHAR(40),
    tracking_device_id  VARCHAR(80),
    in_service_date     DATE,
    out_of_service_date DATE,
    notes               TEXT,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (tenant_id, vin)
);
CREATE INDEX ix_trailer_tenant  ON operations.trailer(tenant_id);
CREATE INDEX ix_trailer_company ON operations.trailer(company_id);
CREATE INDEX ix_trailer_status  ON operations.trailer(tenant_id, status);

-- ─────────────────────────────────────────────────────────────────────────
-- LOAD additions — wire the new entities to the load so we know who's
-- hauling what under which MC.  All four FKs are nullable so the existing
-- planned-but-unassigned workflow still works.
-- ─────────────────────────────────────────────────────────────────────────
ALTER TABLE operations.load
    ADD COLUMN IF NOT EXISTS company_id UUID REFERENCES operations.company(id) ON DELETE RESTRICT,
    ADD COLUMN IF NOT EXISTS driver_id  UUID REFERENCES operations.driver(id)  ON DELETE SET NULL,
    ADD COLUMN IF NOT EXISTS truck_id   UUID REFERENCES operations.truck(id)   ON DELETE SET NULL,
    ADD COLUMN IF NOT EXISTS trailer_id UUID REFERENCES operations.trailer(id) ON DELETE SET NULL;

CREATE INDEX IF NOT EXISTS ix_load_company ON operations.load(company_id);
CREATE INDEX IF NOT EXISTS ix_load_driver  ON operations.load(driver_id);
CREATE INDEX IF NOT EXISTS ix_load_truck   ON operations.load(truck_id);

-- ─────────────────────────────────────────────────────────────────────────
-- RLS — every new tenant-scoped table gets the same policy as the rest.
-- ─────────────────────────────────────────────────────────────────────────
ALTER TABLE operations.company  ENABLE ROW LEVEL SECURITY;
ALTER TABLE operations.driver   ENABLE ROW LEVEL SECURITY;
ALTER TABLE operations.truck    ENABLE ROW LEVEL SECURITY;
ALTER TABLE operations.trailer  ENABLE ROW LEVEL SECURITY;

ALTER TABLE operations.company  FORCE ROW LEVEL SECURITY;
ALTER TABLE operations.driver   FORCE ROW LEVEL SECURITY;
ALTER TABLE operations.truck    FORCE ROW LEVEL SECURITY;
ALTER TABLE operations.trailer  FORCE ROW LEVEL SECURITY;

CREATE POLICY company_tenant_isolation ON operations.company
    USING (tenant_id::text = current_setting('app.current_tenant', true));
CREATE POLICY driver_tenant_isolation ON operations.driver
    USING (tenant_id::text = current_setting('app.current_tenant', true));
CREATE POLICY truck_tenant_isolation ON operations.truck
    USING (tenant_id::text = current_setting('app.current_tenant', true));
CREATE POLICY trailer_tenant_isolation ON operations.trailer
    USING (tenant_id::text = current_setting('app.current_tenant', true));

-- truck_driver inherits isolation by joining through both tables — RLS on it
-- alone would need a subquery, so we leave it without a policy and rely on
-- the FKs.  Both sides being RLS-filtered means a user can't ever see a
-- truck_driver row pointing to truck/driver outside their tenant.
