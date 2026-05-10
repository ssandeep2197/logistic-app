--liquibase formatted sql
--changeset tms:0001-identity-core
--comment: tenants, users, groups, roles, permissions tables.

CREATE TABLE identity.tenant (
    id              UUID PRIMARY KEY,
    version         BIGINT NOT NULL DEFAULT 0,
    slug            VARCHAR(64) NOT NULL UNIQUE,
    name            VARCHAR(200) NOT NULL,
    plan            VARCHAR(40) NOT NULL DEFAULT 'starter',
    status          VARCHAR(20) NOT NULL DEFAULT 'active',
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE identity.app_user (
    id              UUID PRIMARY KEY,
    version         BIGINT NOT NULL DEFAULT 0,
    tenant_id       UUID NOT NULL REFERENCES identity.tenant(id) ON DELETE CASCADE,
    email           VARCHAR(320) NOT NULL,
    password_hash   VARCHAR(200) NOT NULL,
    full_name       VARCHAR(200),
    status          VARCHAR(20) NOT NULL DEFAULT 'active',
    last_login_at   TIMESTAMPTZ,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (tenant_id, email)
);
CREATE INDEX ix_app_user_tenant ON identity.app_user(tenant_id);

CREATE TABLE identity.permission (
    id              UUID PRIMARY KEY,
    version         BIGINT NOT NULL DEFAULT 0,
    -- System-defined; no tenant_id. Same set of permission rows visible to every tenant.
    resource        VARCHAR(80) NOT NULL,
    action          VARCHAR(40) NOT NULL,
    scope           VARCHAR(40) NOT NULL,
    description     TEXT,
    UNIQUE (resource, action, scope)
);

CREATE TABLE identity.role (
    id              UUID PRIMARY KEY,
    version         BIGINT NOT NULL DEFAULT 0,
    tenant_id       UUID NOT NULL REFERENCES identity.tenant(id) ON DELETE CASCADE,
    name            VARCHAR(100) NOT NULL,
    description     TEXT,
    is_system       BOOLEAN NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (tenant_id, name)
);

CREATE TABLE identity.role_permission (
    role_id         UUID NOT NULL REFERENCES identity.role(id) ON DELETE CASCADE,
    permission_id   UUID NOT NULL REFERENCES identity.permission(id) ON DELETE CASCADE,
    PRIMARY KEY (role_id, permission_id)
);

CREATE TABLE identity.app_group (
    id              UUID PRIMARY KEY,
    version         BIGINT NOT NULL DEFAULT 0,
    tenant_id       UUID NOT NULL REFERENCES identity.tenant(id) ON DELETE CASCADE,
    name            VARCHAR(100) NOT NULL,
    description     TEXT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (tenant_id, name)
);

CREATE TABLE identity.group_role (
    group_id        UUID NOT NULL REFERENCES identity.app_group(id) ON DELETE CASCADE,
    role_id         UUID NOT NULL REFERENCES identity.role(id) ON DELETE CASCADE,
    PRIMARY KEY (group_id, role_id)
);

CREATE TABLE identity.user_group (
    user_id         UUID NOT NULL REFERENCES identity.app_user(id) ON DELETE CASCADE,
    group_id        UUID NOT NULL REFERENCES identity.app_group(id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, group_id)
);

-- Direct user→role assignment, for the small number of users that need an
-- exception not modeled by a group.  Permissions = (groups → roles) ∪ direct roles.
CREATE TABLE identity.user_role (
    user_id         UUID NOT NULL REFERENCES identity.app_user(id) ON DELETE CASCADE,
    role_id         UUID NOT NULL REFERENCES identity.role(id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, role_id)
);

-- Optional branch scoping: which branches a user is allowed to operate in.
CREATE TABLE identity.user_branch (
    user_id         UUID NOT NULL REFERENCES identity.app_user(id) ON DELETE CASCADE,
    branch_id       UUID NOT NULL,
    PRIMARY KEY (user_id, branch_id)
);

-- Refresh-token store: keep server-side so we can revoke on password change /
-- forced logout.  Hashing the token at rest means a DB leak doesn't grant access.
CREATE TABLE identity.refresh_token (
    id              UUID PRIMARY KEY,
    version         BIGINT NOT NULL DEFAULT 0,
    user_id         UUID NOT NULL REFERENCES identity.app_user(id) ON DELETE CASCADE,
    tenant_id       UUID NOT NULL REFERENCES identity.tenant(id) ON DELETE CASCADE,
    token_hash      VARCHAR(200) NOT NULL,
    expires_at      TIMESTAMPTZ NOT NULL,
    revoked_at      TIMESTAMPTZ,
    user_agent      VARCHAR(500),
    ip              INET,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX ix_refresh_token_user ON identity.refresh_token(user_id);
CREATE INDEX ix_refresh_token_hash ON identity.refresh_token(token_hash);
