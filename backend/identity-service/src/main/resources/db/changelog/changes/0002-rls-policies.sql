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
-- the policy on `tenant` is permissive when the GUC is unset.
--   * `current_setting('app.current_tenant', true)` returns NULL when the GUC
--     has not been set (missing_ok=true). Plain `= ''` against NULL yields
--     NULL → row denied, which broke bootstrap. We coalesce to '' so the
--     unset case becomes truthy.
--   * Once a tenant context is active, RlsGucInterceptor sets the GUC and
--     only the matching tenant row is visible.
-- Without WITH CHECK, Postgres applies USING to inserts as well; the same
-- coalesce makes inserts work in both modes.
CREATE POLICY tenant_isolation ON identity.tenant
    USING (
        id::text = coalesce(current_setting('app.current_tenant', true), '')
        OR coalesce(current_setting('app.current_tenant', true), '') = ''
    );

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
