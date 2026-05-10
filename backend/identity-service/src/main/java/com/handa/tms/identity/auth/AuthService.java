package com.handa.tms.identity.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.handa.tms.identity.domain.*;
import com.handa.tms.platform.core.auth.AuthPrincipal;
import com.handa.tms.platform.core.error.ConflictException;
import com.handa.tms.platform.core.error.DomainException;
import com.handa.tms.platform.core.outbox.OutboxEvent;
import com.handa.tms.platform.core.tenant.TenantContext;
import com.handa.tms.platform.messaging.outbox.OutboxEventRepository;
import com.handa.tms.platform.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Owns the auth flow: signup, login, refresh, logout.
 * <p>
 * Signup creates a tenant + the first admin user atomically and emits a
 * {@code tenant.created} + {@code user.created} pair via the outbox so other
 * services can build their projections.
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final TenantRepository tenants;
    private final AppUserRepository users;
    private final RoleRepository roles;
    private final RefreshTokenRepository refreshTokens;
    private final OutboxEventRepository outbox;
    private final PasswordHasher hasher;
    private final PermissionResolver permissionResolver;
    private final JwtService jwt;
    private final ObjectMapper json;

    public record SignupResult(UUID tenantId, UUID userId, String accessToken, String refreshToken) {}
    public record LoginResult(UUID tenantId, UUID userId, String accessToken, String refreshToken) {}

    /**
     * Bootstrap a brand-new tenant.  No tenant context is required at the
     * boundary; the policy on {@code identity.tenant} permits insert with an
     * unset GUC for this exact reason.
     */
    @Transactional
    public SignupResult signup(String tenantSlug, String tenantName, String email, String password, String fullName) {
        if (tenants.existsBySlug(tenantSlug)) {
            throw new ConflictException("tenant slug already taken: " + tenantSlug);
        }

        Tenant tenant = tenants.save(Tenant.create(tenantSlug, tenantName));

        // Switch into the new tenant for the rest of the transaction so RLS
        // lets us insert the user, roles, etc.
        return TenantContext.runAs(tenant.getId(), () -> {
            // Seed the "Tenant Admin" role with everything *:*:all.
            Role admin = roles.save(Role.create("Tenant Admin", "Full access to this tenant"));
            admin.setSystemRole(true);
            // Permission seeding: leave roles empty here — frontend's first-time setup wizard
            // assigns permissions after the user picks a plan.  Bootstrap user gets the role
            // and we will let the wizard fill in permissions.

            AppUser user = AppUser.create(email, hasher.hash(password), fullName);
            user.getDirectRoles().add(admin);
            user = users.save(user);

            String access = jwt.issueAccessToken(toPrincipal(user));
            String refresh = issueAndStoreRefresh(user);

            emitTenantCreated(tenant);
            emitUserCreated(user);

            return new SignupResult(tenant.getId(), user.getId(), access, refresh);
        });
    }

    @Transactional
    public LoginResult login(String tenantSlug, String email, String password) {
        Tenant tenant = tenants.findBySlug(tenantSlug)
                .orElseThrow(() -> new DomainException("invalid_credentials", 401, "invalid credentials"));
        return TenantContext.runAs(tenant.getId(), () -> {
            AppUser user = users.findByEmail(email)
                    .orElseThrow(() -> new DomainException("invalid_credentials", 401, "invalid credentials"));
            if (!"active".equals(user.getStatus())) {
                throw new DomainException("user_inactive", 403, "user is not active");
            }
            if (!hasher.matches(password, user.getPasswordHash())) {
                throw new DomainException("invalid_credentials", 401, "invalid credentials");
            }
            user.setLastLoginAt(Instant.now());

            String access = jwt.issueAccessToken(toPrincipal(user));
            String refresh = issueAndStoreRefresh(user);
            return new LoginResult(tenant.getId(), user.getId(), access, refresh);
        });
    }

    @Transactional
    public LoginResult refresh(String refreshToken) {
        JwtService.RefreshClaims claims;
        try {
            claims = jwt.parseRefreshToken(refreshToken);
        } catch (Exception e) {
            throw new DomainException("invalid_refresh", 401, "invalid refresh token");
        }
        return TenantContext.runAs(claims.tenantId(), () -> {
            RefreshToken stored = refreshTokens.findByTokenHash(sha256(refreshToken))
                    .orElseThrow(() -> new DomainException("invalid_refresh", 401, "refresh token not recognised"));
            if (!stored.isActive()) {
                throw new DomainException("invalid_refresh", 401, "refresh token expired or revoked");
            }
            // Token rotation: revoke the used token, issue a new pair.
            stored.setRevokedAt(Instant.now());

            AppUser user = users.findById(claims.userId())
                    .orElseThrow(() -> new DomainException("invalid_refresh", 401, "user not found"));

            String access = jwt.issueAccessToken(toPrincipal(user));
            String newRefresh = issueAndStoreRefresh(user);
            return new LoginResult(claims.tenantId(), user.getId(), access, newRefresh);
        });
    }

    @Transactional
    public void logout(String refreshToken) {
        try {
            JwtService.RefreshClaims claims = jwt.parseRefreshToken(refreshToken);
            TenantContext.runAs(claims.tenantId(), () -> {
                refreshTokens.findByTokenHash(sha256(refreshToken))
                        .ifPresent(rt -> rt.setRevokedAt(Instant.now()));
                return null;
            });
        } catch (Exception ignored) {
            // Logout should always succeed from the caller's perspective.
        }
    }

    private String issueAndStoreRefresh(AppUser user) {
        String tokenStr = jwt.issueRefreshToken(user.getId(), user.getTenantId());
        RefreshToken rt = new RefreshToken();
        rt.setId(UUID.randomUUID());
        rt.setUserId(user.getId());
        rt.setTenantId(user.getTenantId());
        rt.setTokenHash(sha256(tokenStr));
        rt.setExpiresAt(Instant.now().plus(java.time.Duration.ofDays(30)));
        refreshTokens.save(rt);
        return tokenStr;
    }

    private AuthPrincipal toPrincipal(AppUser user) {
        return new AuthPrincipal(
                user.getId(),
                user.getTenantId(),
                user.getEmail(),
                permissionResolver.resolve(user),
                user.getBranchIds());
    }

    private void emitTenantCreated(Tenant t) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("id", t.getId().toString());
        payload.put("slug", t.getSlug());
        payload.put("name", t.getName());
        payload.put("plan", t.getPlan());
        payload.put("status", t.getStatus());
        outbox.save(OutboxEvent.of("tenant.created.v1", t.getId().toString(), writeJson(payload), null));
    }

    private void emitUserCreated(AppUser u) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("id", u.getId().toString());
        payload.put("tenantId", u.getTenantId().toString());
        payload.put("email", u.getEmail());
        payload.put("fullName", u.getFullName());
        payload.put("status", u.getStatus());
        outbox.save(OutboxEvent.of("user.created.v1", u.getId().toString(), writeJson(payload), u.getTenantId()));
    }

    private String writeJson(Object o) {
        try { return json.writeValueAsString(o); }
        catch (Exception e) { throw new IllegalStateException(e); }
    }

    private String sha256(String s) {
        try {
            byte[] d = MessageDigest.getInstance("SHA-256").digest(s.getBytes());
            StringBuilder sb = new StringBuilder(d.length * 2);
            for (byte b : d) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }
}
