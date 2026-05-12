--liquibase formatted sql
--changeset tms:operations-0003-rls
--comment: Per-tenant isolation on operations tables.  Same shape as identity-service:
--         the GUC app.current_tenant is set per @Transactional method by
--         RlsGucInterceptor in platform-lib-security.  Empty/unset GUC = no
--         rows visible.  No bootstrap exception needed (every operations
--         write happens with a tenant context already established).

ALTER TABLE operations.customer    ENABLE ROW LEVEL SECURITY;
ALTER TABLE operations.load        ENABLE ROW LEVEL SECURITY;
ALTER TABLE operations.outbox_event ENABLE ROW LEVEL SECURITY;

ALTER TABLE operations.customer    FORCE ROW LEVEL SECURITY;
ALTER TABLE operations.load        FORCE ROW LEVEL SECURITY;
-- outbox_event tenant_id is nullable for system-wide events; loosen the
-- policy so service-internal jobs (no tenant in context) can publish them.
ALTER TABLE operations.outbox_event FORCE ROW LEVEL SECURITY;

CREATE POLICY customer_tenant_isolation ON operations.customer
    USING (tenant_id::text = current_setting('app.current_tenant', true));

CREATE POLICY load_tenant_isolation ON operations.load
    USING (tenant_id::text = current_setting('app.current_tenant', true));

CREATE POLICY outbox_tenant_isolation ON operations.outbox_event
    USING (
        tenant_id IS NULL
        OR tenant_id::text = COALESCE(current_setting('app.current_tenant', true), '')
        OR COALESCE(current_setting('app.current_tenant', true), '') = ''
    );
