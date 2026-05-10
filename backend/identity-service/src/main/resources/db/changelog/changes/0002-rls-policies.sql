--liquibase formatted sql
--changeset tms:0002-identity-rls
--comment: row-level security on every tenant-scoped table.  Enforced by the GUC `app.current_tenant`,
--         which is set per-transaction by RlsGucInterceptor in platform-lib-security.

ALTER TABLE identity.tenant       ENABLE ROW LEVEL SECURITY;
ALTER TABLE identity.app_user     ENABLE ROW LEVEL SECURITY;
ALTER TABLE identity.role         ENABLE ROW LEVEL SECURITY;
ALTER TABLE identity.app_group    ENABLE ROW LEVEL SECURITY;
ALTER TABLE identity.refresh_token ENABLE ROW LEVEL SECURITY;

-- The signup flow needs to insert the FIRST tenant before any user exists, so
-- the policy on `tenant` allows insert when the GUC is unset, but reads only
-- when the GUC matches the row's id.
CREATE POLICY tenant_isolation ON identity.tenant
    USING (id::text = current_setting('app.current_tenant', true)
        OR current_setting('app.current_tenant', true) = '');

-- All other tenant tables: GUC must match.  Empty GUC = no rows visible.
CREATE POLICY user_tenant_isolation ON identity.app_user
    USING (tenant_id::text = current_setting('app.current_tenant', true));
CREATE POLICY role_tenant_isolation ON identity.role
    USING (tenant_id::text = current_setting('app.current_tenant', true));
CREATE POLICY group_tenant_isolation ON identity.app_group
    USING (tenant_id::text = current_setting('app.current_tenant', true));
CREATE POLICY refresh_token_tenant_isolation ON identity.refresh_token
    USING (tenant_id::text = current_setting('app.current_tenant', true));

-- Service role bypasses RLS for migrations + admin tasks.  Production should
-- run the application as a separate non-bypass role; only DDL/migration runs
-- as the bypassing owner.
ALTER TABLE identity.tenant FORCE ROW LEVEL SECURITY;
ALTER TABLE identity.app_user FORCE ROW LEVEL SECURITY;
ALTER TABLE identity.role FORCE ROW LEVEL SECURITY;
ALTER TABLE identity.app_group FORCE ROW LEVEL SECURITY;
ALTER TABLE identity.refresh_token FORCE ROW LEVEL SECURITY;
