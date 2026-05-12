--liquibase formatted sql
--changeset tms:0006-identity-oauth-updated-at
--comment: Adds the updated_at column that TenantScopedEntity expects.  0005
--         was committed without it; once that ships to a DB this changeset
--         catches up.  In environments that ran 0005 with the fixed version
--         (column already exists), this changeset becomes a no-op via IF NOT EXISTS.

ALTER TABLE identity.oauth_identity
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ NOT NULL DEFAULT now();
