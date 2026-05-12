package com.helloworlds.tms.identity.web;

import com.helloworlds.tms.identity.auth.GoogleOAuthProperties;
import com.helloworlds.tms.identity.auth.GoogleOAuthService;
import com.helloworlds.tms.identity.auth.GoogleOAuthService.Authenticated;
import com.helloworlds.tms.identity.auth.GoogleOAuthService.NeedsWorkspace;
import com.helloworlds.tms.platform.core.error.DomainException;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * Endpoints for the Google OAuth flow.  All routes are PUBLIC — they're how
 * an unauthenticated user logs in / signs up.
 *
 * <ul>
 *   <li>GET /auth/oauth/google/start?mode=login|signup&amp;tenantSlug=...
 *       → 302 redirect to Google.  tenantSlug is optional in signup mode.</li>
 *   <li>GET /auth/oauth/google/callback?code=…&amp;state=…
 *       (Google redirects here)
 *       → 302 to /login/oauth-callback#…  on successful login,
 *       → 302 to /signup/workspace#pendingSignup=… when a new user needs to
 *         pick a workspace slug.</li>
 *   <li>POST /auth/oauth/google/complete-signup
 *       JSON body {tenantSlug, pendingSignupToken, fullName?}.  Returns the
 *       same TokenResponse as /auth/login.</li>
 * </ul>
 */
@RestController
@RequestMapping("/auth/oauth/google")
@RequiredArgsConstructor
public class OAuthController {

    private static final Logger log = LoggerFactory.getLogger(OAuthController.class);

    private final GoogleOAuthService google;
    private final GoogleOAuthProperties config;

    @GetMapping("/start")
    public void start(@RequestParam(required = false) String tenantSlug,
                      @RequestParam(defaultValue = "login") String mode,
                      HttpServletResponse res) throws IOException {
        try {
            String url = google.buildAuthorizationUrl(tenantSlug, mode);
            res.sendRedirect(url);
        } catch (DomainException e) {
            log.info("OAuth start refused: {} ({})", e.getCode(), e.getMessage());
            redirectError(res, e.getCode());
        }
    }

    @GetMapping("/callback")
    public void callback(@RequestParam(required = false) String code,
                         @RequestParam(required = false) String state,
                         @RequestParam(required = false) String error,
                         HttpServletResponse res) throws IOException {
        if (error != null) {
            redirectError(res, "google_" + error);
            return;
        }
        if (code == null || state == null) {
            redirectError(res, "oauth_missing_params");
            return;
        }
        try {
            var result = google.handleCallback(code, state);
            if (result instanceof Authenticated a) {
                redirectAuthenticated(res, a);
            } else if (result instanceof NeedsWorkspace n) {
                redirectNeedsWorkspace(res, n);
            } else {
                redirectError(res, "oauth_unexpected");
            }
        } catch (DomainException e) {
            log.info("OAuth callback failed: {} ({})", e.getCode(), e.getMessage());
            redirectError(res, e.getCode());
        } catch (Exception e) {
            log.error("OAuth callback unexpected error", e);
            redirectError(res, "oauth_unexpected");
        }
    }

    /**
     * Finishes a Google-signup that started with no tenantSlug.  The frontend
     * collects the slug post-Google and posts here with the pendingSignup
     * JWT we issued.  Returns the standard TokenResponse — frontend stuffs
     * the tokens into localStorage and navigates to /admin, identical to a
     * normal sign-in.
     */
    @PostMapping("/complete-signup")
    public TokenResponse completeSignup(@Valid @RequestBody CompleteSignupRequest req) {
        var r = google.completeSignup(req.tenantSlug(), req.pendingSignupToken(), req.fullName());
        return new TokenResponse(r.tenantId(), r.userId(), r.accessToken(), r.refreshToken());
    }

    // ── DTOs ──────────────────────────────────────────────────────────────

    public record CompleteSignupRequest(
            @NotBlank @Size(min = 2, max = 64) String tenantSlug,
            @NotBlank String pendingSignupToken,
            @Size(max = 200) String fullName) {}

    public record TokenResponse(UUID tenantId, UUID userId, String accessToken, String refreshToken) {}

    // ── Redirect helpers ──────────────────────────────────────────────────

    private void redirectAuthenticated(HttpServletResponse res, Authenticated a) throws IOException {
        String location = config.successUrl()
                + "#accessToken="  + urlEncode(a.result().accessToken())
                + "&refreshToken=" + urlEncode(a.result().refreshToken())
                + "&tid="          + a.result().tenantId()
                + "&uid="          + a.result().userId();
        res.sendRedirect(location);
    }

    private void redirectNeedsWorkspace(HttpServletResponse res, NeedsWorkspace n) throws IOException {
        // Email + name go in the fragment too so the workspace page can
        // pre-fill a sensible slug suggestion without an extra round-trip.
        String location = config.workspaceSetupUrl()
                + "#pendingSignup=" + urlEncode(n.signup().pendingToken())
                + "&email="         + urlEncode(n.signup().email() == null ? "" : n.signup().email())
                + "&name="          + urlEncode(n.signup().name()  == null ? "" : n.signup().name());
        res.sendRedirect(location);
    }

    private void redirectError(HttpServletResponse res, String code) throws IOException {
        res.sendRedirect(config.errorUrl() + "?oauth_error=" + urlEncode(code));
    }

    private static String urlEncode(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }
}
