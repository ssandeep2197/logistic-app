package com.helloworlds.tms.identity.platform;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Cross-tenant queries for the platform-owner dashboard.  Runs against the
 * {@link PlatformDataSourceConfig#platformDataSource(String, String, String) BYPASSRLS}
 * connection so it can read every tenant.
 * <p>
 * Read-only.  All writes still go through the default DataSource where RLS
 * applies — even an accidental call from here couldn't write.
 */
@Service
@RequiredArgsConstructor
public class PlatformAdminService {

    private final @Qualifier("platformJdbc") JdbcTemplate jdbc;

    public Stats stats() {
        return jdbc.queryForObject("""
            SELECT
              (SELECT count(*) FROM identity.tenant)                                                AS tenants,
              (SELECT count(*) FROM identity.tenant WHERE status='active')                          AS tenants_active,
              (SELECT count(*) FROM identity.tenant WHERE created_at > now() - interval '7 days')   AS tenants_new_7d,
              (SELECT count(*) FROM identity.tenant WHERE created_at > now() - interval '30 days')  AS tenants_new_30d,
              (SELECT count(*) FROM identity.app_user)                                              AS users,
              (SELECT count(*) FROM identity.app_user WHERE status='active')                        AS users_active,
              (SELECT count(*) FROM identity.app_user WHERE created_at > now() - interval '7 days') AS users_new_7d,
              (SELECT count(*) FROM identity.app_user WHERE created_at > now() - interval '30 days') AS users_new_30d,
              (SELECT count(*) FROM identity.oauth_identity WHERE provider='google')                 AS users_with_google,
              (SELECT count(*) FROM identity.refresh_token
                 WHERE revoked_at IS NULL AND expires_at > now())                                    AS active_sessions,
              (SELECT count(DISTINCT u.id) FROM identity.app_user u
                 WHERE u.last_login_at > now() - interval '30 days')                                 AS users_active_30d
            """,
            (rs, n) -> new Stats(
                rs.getLong("tenants"),         rs.getLong("tenants_active"),
                rs.getLong("tenants_new_7d"),  rs.getLong("tenants_new_30d"),
                rs.getLong("users"),           rs.getLong("users_active"),
                rs.getLong("users_new_7d"),    rs.getLong("users_new_30d"),
                rs.getLong("users_with_google"),
                rs.getLong("active_sessions"),
                rs.getLong("users_active_30d")
            ));
    }

    /** Returns at most {@code limit} tenants ordered by most recently created. */
    public List<TenantRow> recentTenants(int limit) {
        return jdbc.query("""
            SELECT t.id, t.slug, t.name, t.plan, t.status, t.created_at,
                   (SELECT count(*) FROM identity.app_user u WHERE u.tenant_id = t.id) AS user_count,
                   (SELECT max(last_login_at) FROM identity.app_user u WHERE u.tenant_id = t.id) AS last_login
              FROM identity.tenant t
             ORDER BY t.created_at DESC
             LIMIT ?
            """,
            (rs, n) -> new TenantRow(
                (UUID) rs.getObject("id"),
                rs.getString("slug"),
                rs.getString("name"),
                rs.getString("plan"),
                rs.getString("status"),
                rs.getTimestamp("created_at").toInstant(),
                rs.getLong("user_count"),
                rs.getTimestamp("last_login") == null ? null
                        : rs.getTimestamp("last_login").toInstant()
            ),
            limit);
    }

    /** Returns at most {@code limit} users ordered by most recently created. */
    public List<UserRow> recentUsers(int limit) {
        return jdbc.query("""
            SELECT u.id, u.tenant_id, t.slug AS tenant_slug, u.email, u.full_name,
                   u.status, u.created_at, u.last_login_at, u.is_platform_admin,
                   EXISTS (SELECT 1 FROM identity.oauth_identity oi
                            WHERE oi.user_id = u.id AND oi.provider='google') AS has_google
              FROM identity.app_user u
              JOIN identity.tenant t ON t.id = u.tenant_id
             ORDER BY u.created_at DESC
             LIMIT ?
            """,
            (rs, n) -> new UserRow(
                (UUID) rs.getObject("id"),
                (UUID) rs.getObject("tenant_id"),
                rs.getString("tenant_slug"),
                rs.getString("email"),
                rs.getString("full_name"),
                rs.getString("status"),
                rs.getTimestamp("created_at").toInstant(),
                rs.getTimestamp("last_login_at") == null ? null
                        : rs.getTimestamp("last_login_at").toInstant(),
                rs.getBoolean("is_platform_admin"),
                rs.getBoolean("has_google")
            ),
            limit);
    }

    // ── DTOs returned over the wire ───────────────────────────────────────

    public record Stats(
            long tenants,
            long tenantsActive,
            long tenantsNew7d,
            long tenantsNew30d,
            long users,
            long usersActive,
            long usersNew7d,
            long usersNew30d,
            long usersWithGoogle,
            long activeSessions,
            long usersActive30d) {}

    public record TenantRow(
            UUID id,
            String slug,
            String name,
            String plan,
            String status,
            Instant createdAt,
            long userCount,
            Instant lastLogin) {}

    public record UserRow(
            UUID id,
            UUID tenantId,
            String tenantSlug,
            String email,
            String fullName,
            String status,
            Instant createdAt,
            Instant lastLoginAt,
            boolean platformAdmin,
            boolean hasGoogle) {}
}
