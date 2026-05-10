--liquibase formatted sql
--changeset tms:0003-seed-permissions runOnChange:true
--comment: System permissions catalog. Seeded once; tenants reference these by id when assembling roles.

INSERT INTO identity.permission (id, resource, action, scope, description) VALUES
    ('00000000-0000-0000-0000-000000000001', 'tenant',     'read',   'all',        'Read tenant settings'),
    ('00000000-0000-0000-0000-000000000002', 'tenant',     'update', 'all',        'Update tenant settings'),
    ('00000000-0000-0000-0000-000000000003', 'user',       'read',   'all',        'Read any user in tenant'),
    ('00000000-0000-0000-0000-000000000004', 'user',       'create', 'all',        'Invite new users'),
    ('00000000-0000-0000-0000-000000000005', 'user',       'update', 'all',        'Edit any user in tenant'),
    ('00000000-0000-0000-0000-000000000006', 'user',       'delete', 'all',        'Deactivate users'),
    ('00000000-0000-0000-0000-000000000007', 'role',       'manage', 'all',        'Create/edit roles and permissions'),
    ('00000000-0000-0000-0000-000000000008', 'group',      'manage', 'all',        'Create/edit groups and assignments'),
    ('00000000-0000-0000-0000-000000000010', 'load',       'read',   'all',        'View any load in tenant'),
    ('00000000-0000-0000-0000-000000000011', 'load',       'read',   'own_branch', 'View loads in own branches'),
    ('00000000-0000-0000-0000-000000000012', 'load',       'read',   'own',        'View own loads only'),
    ('00000000-0000-0000-0000-000000000013', 'load',       'create', 'all',        'Create loads'),
    ('00000000-0000-0000-0000-000000000014', 'load',       'update', 'all',        'Edit any load'),
    ('00000000-0000-0000-0000-000000000015', 'load',       'update', 'own_branch', 'Edit loads in own branches'),
    ('00000000-0000-0000-0000-000000000016', 'load',       'update', 'own',        'Edit own loads'),
    ('00000000-0000-0000-0000-000000000020', 'invoice',    'read',   'all',        'View any invoice'),
    ('00000000-0000-0000-0000-000000000021', 'invoice',    'create', 'all',        'Create invoices'),
    ('00000000-0000-0000-0000-000000000022', 'payment',    'create', 'all',        'Apply payments'),
    ('00000000-0000-0000-0000-000000000030', 'payroll',    'manage', 'all',        'Run payroll, edit pay rates'),
    ('00000000-0000-0000-0000-000000000031', 'driver_pay', 'read',   'own',        'View own driver settlements'),
    ('00000000-0000-0000-0000-000000000040', 'report',     'view',   'all',        'View any report'),
    ('00000000-0000-0000-0000-000000000041', 'report',     'build',  'all',        'Create custom reports'),
    ('00000000-0000-0000-0000-000000000050', 'tracking',   'view',   'all',        'See live truck positions')
ON CONFLICT (resource, action, scope) DO UPDATE SET description = EXCLUDED.description;
