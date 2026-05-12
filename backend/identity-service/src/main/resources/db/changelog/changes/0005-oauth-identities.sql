--liquibase formatted sql
--changeset tms:0005-identity-oauth
--comment: External-identity table linking (provider, subject) to a tenant's AppUser.
--         Subject is the IdP's immutable user id (Google's "sub", GitHub's id,
--         Microsoft's oid); never email — emails can change.

CREATE TABLE identity.oauth_identity (
    id              UUID PRIMARY KEY,
    version         BIGINT NOT NULL DEFAULT 0,
    tenant_id       UUID NOT NULL REFERENCES identity.tenant(id) ON DELETE CASCADE,
    user_id         UUID NOT NULL REFERENCES identity.app_user(id) ON DELETE CASCADE,
    provider        VARCHAR(40) NOT NULL,   -- 'google', 'github', 'microsoft', ...
    subject         VARCHAR(255) NOT NULL,  -- Provider's stable user id
    email_at_link   VARCHAR(320),           -- Captured at link time, for display only
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    last_used_at    TIMESTAMPTZ,
    -- A given external identity maps to at most ONE TMS user across the
    -- entire system.  (Two tenants cannot both claim the same Google account.)
    UNIQUE (provider, subject)
);
CREATE INDEX ix_oauth_identity_user ON identity.oauth_identity(user_id);
CREATE INDEX ix_oauth_identity_tenant ON identity.oauth_identity(tenant_id);

-- NO row-level security on this table.  The OAuth callback has to look up
-- the row by (provider, subject) BEFORE it knows which tenant to switch
-- into, so an RLS policy keyed on app.current_tenant would always return
-- nothing.  The UNIQUE (provider, subject) constraint already gives us the
-- security boundary we need: one external identity maps to one TMS user.
-- The table is not exposed via any API endpoint.
