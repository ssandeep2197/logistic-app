package com.helloworlds.tms.identity.auth;

import com.fasterxml.jackson.databind.JsonNode;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Manual OAuth 2.0 / OIDC client for Google.  Done by hand (rather than via
 * spring-boot-starter-oauth2-client) because:
 *   1. We need to carry our own state — tenant slug + login/signup mode —
 *      through the redirect, which doesn't fit Spring's stock state handling.
 *   2. We don't want a server-side session; the success path issues OUR JWT
 *      and the user is stateless from there.
 *   3. The token-exchange response from Google over TLS is trusted, so we
 *      don't need JWKS-based id_token signature verification.
 */
@Service
@RequiredArgsConstructor
public class GoogleOAuthService {

    private static final Logger log = LoggerFactory.getLogger(GoogleOAuthService.class);

    private static final String AUTH_URI    = "https://accounts.google.com/o/oauth2/v2/auth";
    private static final String TOKEN_URI   = "https://oauth2.googleapis.com/token";
    private static final String SCOPES      = "openid email profile";
    private static final String STATE_PUR   = "google-oauth";
    private static final Duration STATE_TTL = Duration.ofMinutes(5);

    private final TenantRepository tenants;
    private final AppUserRepository users;
    private final RoleRepository roles;
    private final PermissionRepository permissions;
    private final OAuthIdentityRepository identities;
    private final RefreshTokenRepository refreshTokens;
    private final OutboxEventRepository outbox;
    private final PermissionResolver permissionResolver;
    private final JwtService jwt;
    private final ObjectMapper json;
    private final GoogleOAuthProperties config;
    private final PasswordHasher hasher;

    @PersistenceContext
    private EntityManager em;

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    public record AuthResult(UUID tenantId, UUID userId, String accessToken, String refreshToken) {}

    // ── Step 1: build the URL we redirect the browser to ───────────────────

    /**
     * Builds the Google authorization URL.  The state token is a signed JWT
     * carrying the tenant slug + mode so the callback can resolve where this
     * request came from without trusting a query parameter.
     */
    public String buildAuthorizationUrl(String tenantSlug, String mode) {
        if (!config.isConfigured()) {
            throw new DomainException("oauth_not_configured", 503,
                    "Google OAuth is not configured on this deployment");
        }
        if (!"login".equals(mode) && !"signup".equals(mode)) {
            throw new DomainException("oauth_bad_mode", 400, "mode must be 'login' or 'signup'");
        }
        if (tenantSlug == null || tenantSlug.isBlank()) {
            throw new DomainException("oauth_missing_tenant", 400, "tenantSlug is required");
        }

        String state = jwt.issueStateToken(STATE_PUR, Map.of(
                "tenantSlug", tenantSlug,
                "mode", mode
        ), STATE_TTL);

        StringBuilder url = new StringBuilder(AUTH_URI);
        url.append("?response_type=code");
        url.append("&client_id=").append(urlencode(config.clientId()));
        url.append("&redirect_uri=").append(urlencode(config.redirectUri()));
        url.append("&scope=").append(urlencode(SCOPES));
        url.append("&state=").append(urlencode(state));
        url.append("&access_type=online");                 // No refresh from Google — we issue our own.
        url.append("&prompt=select_account");
        return url.toString();
    }

    // ── Step 2: handle the callback ───────────────────────────────────────

    /**
     * Exchanges the authorization code for tokens, identifies the user, and
     * issues OUR access + refresh tokens.  Transactional so the new-user
     * creation path (signup) is atomic — tenant + role + user + identity all
     * commit together or not at all.
     */
    @Transactional
    public AuthResult handleCallback(String code, String state) {
        if (!config.isConfigured()) {
            throw new DomainException("oauth_not_configured", 503, "Google OAuth is not configured");
        }

        // Verify state, extract tenant + mode.
        Map<String, String> claims;
        try {
            claims = jwt.parseStateToken(state, STATE_PUR);
        } catch (Exception e) {
            throw new DomainException("oauth_bad_state", 400, "invalid or expired OAuth state");
        }
        String tenantSlug = claims.get("tenantSlug");
        String mode       = claims.get("mode");

        // Exchange the code with Google.
        GoogleTokens gt = exchangeCode(code);

        // Decode the id_token payload — we trust the TLS channel for integrity.
        GoogleIdClaims gid = decodeIdToken(gt.idToken());
        if (!gid.emailVerified()) {
            throw new DomainException("oauth_email_unverified", 400,
                    "Google reports this email is not verified — sign in via Google again with a verified account");
        }

        // Has this Google account ever been linked?
        Optional<OAuthIdentity> existing = identities.findByProviderAndSubject("google", gid.sub());

        if (existing.isPresent()) {
            return loginExisting(existing.get(), tenantSlug);
        }
        if ("login".equals(mode)) {
            throw new DomainException("oauth_no_account", 404,
                    "no TMS account is linked to this Google identity yet — sign up instead");
        }
        // signup mode: create a brand-new tenant + admin user linked to this Google identity.
        return signupNew(tenantSlug, gid);
    }

    private AuthResult loginExisting(OAuthIdentity oi, String tenantSlugFromState) {
        // The identity row carries the canonical tenant; if the state pointed
        // at a different tenant, the user picked the wrong subdomain/form.
        Tenant tenant = tenants.findById(oi.getTenantId())
                .orElseThrow(() -> new DomainException("oauth_tenant_missing", 500,
                        "linked tenant no longer exists"));
        if (tenantSlugFromState != null && !tenantSlugFromState.equalsIgnoreCase(tenant.getSlug())) {
            throw new DomainException("oauth_wrong_tenant", 403,
                    "this Google account is linked to a different tenant");
        }

        setTenantGuc(tenant.getId());
        return TenantContext.runAs(tenant.getId(), () -> {
            AppUser user = users.findById(oi.getUserId())
                    .orElseThrow(() -> new DomainException("oauth_user_missing", 500,
                            "linked user no longer exists"));
            if (!"active".equals(user.getStatus())) {
                throw new DomainException("user_inactive", 403, "user is not active");
            }
            user.setLastLoginAt(Instant.now());
            oi.setLastUsedAt(Instant.now());

            String access  = jwt.issueAccessToken(toPrincipal(user));
            String refresh = issueAndStoreRefresh(user);
            return new AuthResult(tenant.getId(), user.getId(), access, refresh);
        });
    }

    private AuthResult signupNew(String tenantSlug, GoogleIdClaims gid) {
        if (tenants.existsBySlug(tenantSlug)) {
            throw new ConflictException("tenant slug already taken: " + tenantSlug);
        }
        Tenant tenant = tenants.save(Tenant.create(tenantSlug, tenantSlug));

        setTenantGuc(tenant.getId());
        return TenantContext.runAs(tenant.getId(), () -> {
            Role admin = Role.create("Tenant Admin", "Full access to this tenant");
            admin.setSystemRole(true);
            admin.getPermissions().addAll(permissions.findAll());
            admin = roles.save(admin);

            // Random password — the user authenticates via Google.  They can
            // set a real one later from the profile screen if they want a
            // password fallback.
            String randomPw = UUID.randomUUID() + "-" + UUID.randomUUID();
            AppUser user = AppUser.create(gid.email(), hasher.hash(randomPw), gid.name());
            user.getDirectRoles().add(admin);
            user = users.save(user);

            OAuthIdentity oi = OAuthIdentity.create("google", gid.sub(), user.getId(), gid.email());
            identities.save(oi);

            String access  = jwt.issueAccessToken(toPrincipal(user));
            String refresh = issueAndStoreRefresh(user);

            emitTenantCreated(tenant);
            emitUserCreated(user);

            return new AuthResult(tenant.getId(), user.getId(), access, refresh);
        });
    }

    // ── Google REST calls + id_token parsing ──────────────────────────────

    private GoogleTokens exchangeCode(String code) {
        String body = "code="          + urlencode(code)
                + "&client_id="        + urlencode(config.clientId())
                + "&client_secret="    + urlencode(config.clientSecret())
                + "&redirect_uri="     + urlencode(config.redirectUri())
                + "&grant_type=authorization_code";
        HttpRequest req = HttpRequest.newBuilder(URI.create(TOKEN_URI))
                .timeout(Duration.ofSeconds(10))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        try {
            HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (res.statusCode() / 100 != 2) {
                log.warn("Google token exchange failed: {} {}", res.statusCode(), res.body());
                throw new DomainException("oauth_exchange_failed", 502,
                        "Google rejected the OAuth code");
            }
            JsonNode n = json.readTree(res.body());
            String idToken = textOrNull(n, "id_token");
            if (idToken == null) {
                throw new DomainException("oauth_no_id_token", 502, "Google response missing id_token");
            }
            return new GoogleTokens(idToken, textOrNull(n, "access_token"));
        } catch (DomainException e) {
            throw e;
        } catch (Exception e) {
            log.error("Google token exchange transport error", e);
            throw new DomainException("oauth_transport", 502, "could not reach Google to exchange the code");
        }
    }

    private GoogleIdClaims decodeIdToken(String idToken) {
        // Header.Payload.Signature — we read the payload only; the TLS-secured
        // exchange already proved authenticity.
        String[] parts = idToken.split("\\.");
        if (parts.length != 3) {
            throw new DomainException("oauth_bad_id_token", 502, "malformed id_token");
        }
        try {
            byte[] payload = Base64.getUrlDecoder().decode(parts[1]);
            JsonNode n = json.readTree(payload);
            // Defense in depth: validate audience matches our client id.
            String aud = textOrNull(n, "aud");
            if (aud == null || !aud.equals(config.clientId())) {
                throw new DomainException("oauth_bad_audience", 502, "id_token audience does not match");
            }
            return new GoogleIdClaims(
                    textOrNull(n, "sub"),
                    textOrNull(n, "email"),
                    n.path("email_verified").asBoolean(false),
                    textOrNull(n, "name")
            );
        } catch (DomainException e) {
            throw e;
        } catch (Exception e) {
            throw new DomainException("oauth_decode", 502, "could not decode id_token");
        }
    }

    private record GoogleTokens(String idToken, String accessToken) {}
    private record GoogleIdClaims(String sub, String email, boolean emailVerified, String name) {}

    // ── Helpers (mirror AuthService's private helpers; consolidate later) ─

    private AuthPrincipal toPrincipal(AppUser user) {
        return new AuthPrincipal(
                user.getId(),
                user.getTenantId(),
                user.getEmail(),
                permissionResolver.resolve(user),
                user.getBranchIds());
    }

    private String issueAndStoreRefresh(AppUser user) {
        String tokenStr = jwt.issueRefreshToken(user.getId(), user.getTenantId());
        RefreshToken rt = new RefreshToken();
        rt.setId(UUID.randomUUID());
        rt.setUserId(user.getId());
        rt.setTenantId(user.getTenantId());
        rt.setTokenHash(sha256(tokenStr));
        rt.setExpiresAt(Instant.now().plus(Duration.ofDays(30)));
        refreshTokens.save(rt);
        return tokenStr;
    }

    private void setTenantGuc(UUID tenantId) {
        em.createNativeQuery("SELECT set_config('app.current_tenant', :t, true)")
          .setParameter("t", tenantId.toString())
          .getSingleResult();
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

    private static String urlencode(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }

    private static String textOrNull(JsonNode n, String field) {
        JsonNode v = n.path(field);
        return v.isMissingNode() || v.isNull() ? null : v.asText();
    }

    private static String sha256(String s) {
        try {
            byte[] d = java.security.MessageDigest.getInstance("SHA-256").digest(s.getBytes());
            StringBuilder sb = new StringBuilder(d.length * 2);
            for (byte b : d) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) { throw new IllegalStateException(e); }
    }
}
