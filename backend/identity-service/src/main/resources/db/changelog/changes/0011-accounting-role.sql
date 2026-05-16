--liquibase formatted sql
--changeset tms:0011-bank-account-permissions runOnChange:true
--comment: Permissions for the new bank_account resource (operations-service
--         0007).  Deliberately NOT granted to Tenant Admin — the user's
--         verbatim requirement is "only accounting team can edit bank
--         details", so a separate Accounting role is the only role that
--         gets bank_account:* by default.  Future signups filter
--         bank_account:* out of Tenant Admin in AuthService / GoogleOAuthService.

INSERT INTO identity.permission (id, resource, action, scope, description) VALUES
    ('00000000-0000-0000-0000-0000000000b0', 'bank_account', 'read',   'all',
        'List/view masked bank accounts (last 4 only)'),
    ('00000000-0000-0000-0000-0000000000b1', 'bank_account', 'create', 'all',
        'Add a bank account to a company or driver'),
    ('00000000-0000-0000-0000-0000000000b2', 'bank_account', 'update', 'all',
        'Edit a bank account (incl. rotating numbers)'),
    ('00000000-0000-0000-0000-0000000000b3', 'bank_account', 'delete', 'all',
        'Archive a bank account'),
    ('00000000-0000-0000-0000-0000000000b4', 'bank_account', 'reveal', 'all',
        'View unmasked account/routing numbers (audited)')
ON CONFLICT (resource, action, scope) DO UPDATE SET description = EXCLUDED.description;


--changeset tms:0011-accounting-role-per-tenant runOnChange:true
--comment: Seed an "Accounting" system role for every existing tenant.
--         identity.role.tenant_id is NOT NULL — system roles are per-tenant
--         clones, not global rows (see AuthService.signup), so we have to
--         materialise one row per tenant.  AuthService + GoogleOAuthService
--         do the equivalent for new signups going forward.

INSERT INTO identity.role (id, tenant_id, version, name, description, is_system, created_at, updated_at)
SELECT
    gen_random_uuid(),
    t.id,
    0,
    'Accounting',
    'Books, payments, payroll, bank details.  Held separately from Tenant Admin '
        || 'so general admins cannot see employee/driver banking info.',
    TRUE,
    NOW(),
    NOW()
FROM identity.tenant t
WHERE NOT EXISTS (
    SELECT 1 FROM identity.role r
    WHERE r.tenant_id = t.id AND r.name = 'Accounting'
);


--changeset tms:0011-accounting-role-permissions runOnChange:true
--comment: Grant each Accounting role the perms its members need to actually
--         do accounting work: bank_account:*, plus read on the entities they
--         attach accounts to (company, driver, load), plus invoice/payment/payroll.

INSERT INTO identity.role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM identity.role r
CROSS JOIN identity.permission p
WHERE r.is_system = TRUE
  AND r.name = 'Accounting'
  AND (
       p.resource = 'bank_account'
    OR (p.resource = 'invoice'   AND p.scope = 'all')
    OR (p.resource = 'payment'   AND p.scope = 'all')
    OR (p.resource = 'payroll'   AND p.scope = 'all')
    OR (p.resource = 'load'      AND p.action = 'read' AND p.scope = 'all')
    OR (p.resource = 'company'   AND p.action = 'read' AND p.scope = 'all')
    OR (p.resource = 'driver'    AND p.action = 'read' AND p.scope = 'all')
  )
ON CONFLICT DO NOTHING;
