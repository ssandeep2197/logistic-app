--liquibase formatted sql
--changeset tms:0008-platform-tenant-settings
--comment: Two-level feature-flag store — global (platform_setting) and per-tenant
--         (tenant_setting).  Effective value of a flag = platform AND tenant
--         where both default sensibly (platform usually true, tenant usually
--         false so admins opt in).  First user: 'backup.enabled'.

CREATE TABLE identity.platform_setting (
    key         VARCHAR(80) PRIMARY KEY,
    value       TEXT NOT NULL,
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_by  UUID
);

CREATE TABLE identity.tenant_setting (
    id           UUID PRIMARY KEY,
    version      BIGINT NOT NULL DEFAULT 0,
    tenant_id    UUID NOT NULL REFERENCES identity.tenant(id) ON DELETE CASCADE,
    key          VARCHAR(80) NOT NULL,
    value        TEXT NOT NULL,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_by   UUID,
    UNIQUE (tenant_id, key)
);
CREATE INDEX ix_tenant_setting_tenant ON identity.tenant_setting(tenant_id);

ALTER TABLE identity.tenant_setting ENABLE ROW LEVEL SECURITY;
ALTER TABLE identity.tenant_setting FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_setting_isolation ON identity.tenant_setting
    USING (tenant_id::text = current_setting('app.current_tenant', true));

-- Seed: backups enabled at the platform level by default.  Operators flip
-- this to 'false' when they want to pause the nightly CronJob.
INSERT INTO identity.platform_setting (key, value)
VALUES ('backup.enabled', 'true')
ON CONFLICT (key) DO NOTHING;
