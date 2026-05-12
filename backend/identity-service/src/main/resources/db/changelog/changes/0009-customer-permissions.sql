--liquibase formatted sql
--changeset tms:0009-customer-permissions runOnChange:true
--comment: Adds customer:* permissions and backfills the Tenant Admin role
--         in every existing tenant.  New tenants' Tenant Admin gets these
--         automatically via AuthService.signup which adds every permission
--         to the bootstrap role.

INSERT INTO identity.permission (id, resource, action, scope, description) VALUES
    ('00000000-0000-0000-0000-000000000060', 'customer', 'read',   'all', 'View any customer in tenant'),
    ('00000000-0000-0000-0000-000000000061', 'customer', 'create', 'all', 'Create customers'),
    ('00000000-0000-0000-0000-000000000062', 'customer', 'update', 'all', 'Edit any customer'),
    ('00000000-0000-0000-0000-000000000063', 'customer', 'delete', 'all', 'Archive customers')
ON CONFLICT (resource, action, scope) DO UPDATE SET description = EXCLUDED.description;

-- Backfill: any existing Tenant Admin role gets the new customer permissions.
-- Live tenants pick them up on next token refresh (~15 min) or next sign-in.
INSERT INTO identity.role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM identity.role r
CROSS JOIN identity.permission p
WHERE r.is_system = TRUE
  AND r.name = 'Tenant Admin'
  AND p.resource = 'customer'
ON CONFLICT DO NOTHING;
