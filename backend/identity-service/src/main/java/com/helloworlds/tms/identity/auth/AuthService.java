package com.helloworlds.tms.identity.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.helloworlds.tms.identity.domain.*;
import com.helloworlds.tms.platform.core.auth.AuthPrincipal;
import com.helloworlds.tms.platform.core.error.ConflictException;
import com.helloworlds.tms.platform.core.error.DomainException;
import com.helloworlds.tms.platform.core.outbox.OutboxEvent;
import com.helloworlds.tms.platform.core.tenant.TenantContext;
import com.helloworlds.tms.platform.messaging.outbox.OutboxEventRepository;
import com.helloworlds.tms.platform.security.JwtService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
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
    private final PermissionRepository permissions;
    private final RefreshTokenRepository refreshTokens;
    private final OutboxEventRepository outbox;
    private final PasswordHasher hasher;
    private final PermissionResolver permissionResolver;
    private final JwtService jwt;
    private final ObjectMapper json;

    /**
     * RlsGucInterceptor samples {@link TenantContext} ONCE at @Transactional
     * entry, so for endpoints whose tenant is determined mid-method (signup
     * creates the tenant; login looks it up by slug) we have to set the
     * Postgres GUC manually before the next query that depends on it.
     */
    @PersistenceContext
    private EntityManager em;

    public record SignupResult(UUID tenantId, UUID userId, String accessToken, String refreshToken) {}
    public record LoginResult(UUID tenantId, UUID userId, String accessToken, String refreshToken) {}

    /** Set the per-transaction GUC that tenant-isolation RLS policies read. */
    private void setTenantGuc(UUID tenantId) {
        em.createNativeQuery("SELECT set_config('app.current_tenant', :t, true)")
          .setParameter("t", tenantId.toString())
          .getSingleResult();
    }

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
        // lets us insert the user, roles, etc.  Setting the GUC here (in
        // addition to the thread-local) is required: the AOP aspect already
        // ran at method entry with no tenant context, so the GUC is unset.
        setTenantGuc(tenant.getId());
        return TenantContext.runAs(tenant.getId(), () -> {
            // Seed the "Tenant Admin" role with EVERY system permission. This
            // is the only role that exists at signup time — the first user
            // must be able to manage everything in their tenant or the app is
            // unusable until a separate provisioning path exists.
            //
            // Subsequent admins can create more restricted roles via the
            // /roles endpoint and assign them via /groups, removing
            // permissions from the Tenant Admin role on the way if desired
            // (it's not protected against demotion — system_role only blocks
            // delete + rename).
            Role admin = Role.create("Tenant Admin", "Full access to this tenant");
            admin.setSystemRole(true);
            admin.getPermissions().addAll(permissions.findAll());
            admin = roles.save(admin);

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
        setTenantGuc(tenant.getId());
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
        setTenantGuc(claims.tenantId());
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
            setTenantGuc(claims.tenantId());
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
