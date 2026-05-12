--liquibase formatted sql
--changeset tms:0007-platform-admin-flag
--comment: Marks a user as a *platform* admin (operator of the SaaS), not a
--         tenant admin.  Platform admins see across every tenant via the
--         BYPASSRLS Postgres role used by /platform/* endpoints.  The flag
--         goes into the JWT so the frontend can show/hide the menu link.

ALTER TABLE identity.app_user
    ADD COLUMN IF NOT EXISTS is_platform_admin BOOLEAN NOT NULL DEFAULT FALSE;
