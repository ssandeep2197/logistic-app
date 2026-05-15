--liquibase formatted sql
--changeset tms:0010-fleet-permissions runOnChange:true
--comment: Permissions for the new operations entities introduced in
--         operations-service migration 0004 (company, driver, truck, trailer).
--         Existing Tenant Admin roles are backfilled so live tenants get
--         them without re-signing-up.

INSERT INTO identity.permission (id, resource, action, scope, description) VALUES
    -- Company
    ('00000000-0000-0000-0000-000000000070', 'company', 'read',   'all', 'View any company in tenant'),
    ('00000000-0000-0000-0000-000000000071', 'company', 'create', 'all', 'Create operating companies'),
    ('00000000-0000-0000-0000-000000000072', 'company', 'update', 'all', 'Edit any company'),
    ('00000000-0000-0000-0000-000000000073', 'company', 'delete', 'all', 'Archive companies'),
    -- Driver
    ('00000000-0000-0000-0000-000000000080', 'driver',  'read',   'all', 'View any driver'),
    ('00000000-0000-0000-0000-000000000081', 'driver',  'create', 'all', 'Onboard drivers'),
    ('00000000-0000-0000-0000-000000000082', 'driver',  'update', 'all', 'Edit any driver'),
    ('00000000-0000-0000-0000-000000000083', 'driver',  'delete', 'all', 'Terminate drivers'),
    -- Truck
    ('00000000-0000-0000-0000-000000000090', 'truck',   'read',   'all', 'View any truck'),
    ('00000000-0000-0000-0000-000000000091', 'truck',   'create', 'all', 'Add trucks'),
    ('00000000-0000-0000-0000-000000000092', 'truck',   'update', 'all', 'Edit any truck'),
    ('00000000-0000-0000-0000-000000000093', 'truck',   'delete', 'all', 'Retire trucks'),
    -- Trailer
    ('00000000-0000-0000-0000-0000000000a0', 'trailer', 'read',   'all', 'View any trailer'),
    ('00000000-0000-0000-0000-0000000000a1', 'trailer', 'create', 'all', 'Add trailers'),
    ('00000000-0000-0000-0000-0000000000a2', 'trailer', 'update', 'all', 'Edit any trailer'),
    ('00000000-0000-0000-0000-0000000000a3', 'trailer', 'delete', 'all', 'Retire trailers')
ON CONFLICT (resource, action, scope) DO UPDATE SET description = EXCLUDED.description;

INSERT INTO identity.role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM identity.role r
CROSS JOIN identity.permission p
WHERE r.is_system = TRUE
  AND r.name = 'Tenant Admin'
  AND p.resource IN ('company','driver','truck','trailer')
ON CONFLICT DO NOTHING;
