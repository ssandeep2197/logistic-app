--liquibase formatted sql
--changeset tms:operations-0007-pgcrypto runOnChange:false dbms:postgresql
--comment: pgcrypto for column-level encryption of bank account / routing
--         numbers.  The key never lives in Postgres — it's injected from a
--         K8s secret and passed as a query parameter.

CREATE EXTENSION IF NOT EXISTS pgcrypto;


--changeset tms:operations-0007-bank-account
--comment: Bank account for either a Company (operating account) or a Driver
--         (payroll/settlement deposit).  account_number and routing_number
--         are encrypted with pgcrypto AES-256; only the last 4 digits live
--         in plaintext for UI display.  Reading the unmasked number requires
--         bank_account:reveal:all and writes a row to bank_account_audit.
--         Tables live in `operations` schema for now — separation from the
--         rest of operations is enforced by permission family + role, not
--         schema (operations_svc isn't a superuser; CREATE SCHEMA fails).
--         A future accounting-service can carve this out into `finance.*`.

CREATE TABLE operations.bank_account (
    id                          UUID PRIMARY KEY,
    tenant_id                   UUID NOT NULL,
    version                     BIGINT NOT NULL DEFAULT 0,
    created_at                  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at                  TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    -- Owner discriminator (entity_type, entity_id).  No FK because the
    -- target lives in a different table and could later move; integrity
    -- is enforced by the service layer (lookup before insert).
    entity_type                 VARCHAR(20) NOT NULL,                  -- 'company' | 'driver'
    entity_id                   UUID        NOT NULL,

    nickname                    VARCHAR(100),                          -- "Operating", "Payroll", "ACH-1"
    bank_name                   VARCHAR(200),
    account_holder              VARCHAR(200),                          -- name on the account
    account_type                VARCHAR(20),                           -- 'checking' | 'savings'

    -- Sensitive fields — bytea, populated via pgp_sym_encrypt(plain, :key).
    -- Not mapped in the JPA entity; the service writes them via native
    -- query so the cleartext never sits in a Java field that could leak
    -- through a toString() or Jackson serialiser.
    account_number_encrypted    BYTEA,
    routing_number_encrypted    BYTEA,

    -- Display-safe trailing digits (last 4 of each).  String not int so
    -- routing numbers starting with 0 keep the leading zero.
    account_last4               VARCHAR(4),
    routing_last4               VARCHAR(4),

    swift_code                  VARCHAR(20),                           -- international payments
    void_check_document_id      UUID,                                  -- FK to operations.document (Phase 2.3)

    status                      VARCHAR(20) NOT NULL DEFAULT 'active', -- 'active' | 'archived'

    created_by                  UUID,                                  -- identity.user.id (audit)
    updated_by                  UUID,                                  -- identity.user.id (audit)

    CONSTRAINT bank_account_entity_type_chk
        CHECK (entity_type IN ('company','driver')),
    CONSTRAINT bank_account_status_chk
        CHECK (status IN ('active','archived')),
    CONSTRAINT bank_account_active_has_secrets_chk
        CHECK (status <> 'active'
               OR (account_number_encrypted IS NOT NULL
                   AND routing_number_encrypted IS NOT NULL))
);

CREATE INDEX bank_account_tenant_idx ON operations.bank_account (tenant_id);
CREATE INDEX bank_account_owner_idx  ON operations.bank_account (tenant_id, entity_type, entity_id);
CREATE INDEX bank_account_active_idx ON operations.bank_account (tenant_id, entity_type, entity_id) WHERE status = 'active';


--changeset tms:operations-0007-bank-account-audit
--comment: Audit row written on every reveal/create/update/archive of a
--         bank_account.  No payload — just (who, when, what, which row).
--         Tenant-scoped so platform-owner queries can roll up but tenants
--         can't see each other's audit trail.

CREATE TABLE operations.bank_account_audit (
    id              UUID PRIMARY KEY,
    tenant_id       UUID NOT NULL,
    bank_account_id UUID NOT NULL REFERENCES operations.bank_account(id) ON DELETE CASCADE,
    action          VARCHAR(20) NOT NULL,                              -- reveal | create | update | archive
    actor_user_id   UUID,                                              -- identity.user.id (null = system)
    actor_ip        VARCHAR(45),                                       -- v4 + v6 fits
    at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT bank_account_audit_action_chk
        CHECK (action IN ('reveal','create','update','archive'))
);

CREATE INDEX bank_account_audit_tenant_idx ON operations.bank_account_audit (tenant_id, at DESC);
CREATE INDEX bank_account_audit_row_idx    ON operations.bank_account_audit (bank_account_id, at DESC);


--changeset tms:operations-0007-rls
--comment: Tenant isolation, same shape as the other operations.* tables.
--         Reads/writes also pass through PermissionEvaluator gates in the
--         controller — RLS is the second line of defence.

ALTER TABLE operations.bank_account       ENABLE ROW LEVEL SECURITY;
ALTER TABLE operations.bank_account       FORCE  ROW LEVEL SECURITY;
ALTER TABLE operations.bank_account_audit ENABLE ROW LEVEL SECURITY;
ALTER TABLE operations.bank_account_audit FORCE  ROW LEVEL SECURITY;

CREATE POLICY bank_account_tenant_isolation ON operations.bank_account
    USING (tenant_id::text = current_setting('app.current_tenant', true));

CREATE POLICY bank_account_audit_tenant_isolation ON operations.bank_account_audit
    USING (tenant_id::text = current_setting('app.current_tenant', true));
