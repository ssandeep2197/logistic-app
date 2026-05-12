package com.helloworlds.tms.identity.web;

import com.helloworlds.tms.identity.auth.GoogleOAuthProperties;
import com.helloworlds.tms.identity.auth.GoogleOAuthService;
import com.helloworlds.tms.platform.core.error.DomainException;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Endpoints for the Google OAuth flow.  Both routes are PUBLIC — they're how
 * an unauthenticated user logs in / signs up.
 *
 * <ul>
 *   <li>GET /auth/oauth/google/start?tenantSlug=acme&mode=login
 *       → 302 redirect to Google.</li>
 *   <li>GET /auth/oauth/google/callback?code=…&state=…
 *       (Google redirects here)
 *       → 302 redirect to the frontend with tokens in the URL fragment.</li>
 * </ul>
 *
 * On error the controller redirects to the frontend with an
 * {@code ?oauth_error=&lt;code&gt;} query, so the user lands back on the
 * login screen with a message rather than a raw JSON 4xx.
 */
@RestController
@RequestMapping("/auth/oauth/google")
@RequiredArgsConstructor
public class OAuthController {

    private static final Logger log = LoggerFactory.getLogger(OAuthController.class);

    private final GoogleOAuthService google;
    private final GoogleOAuthProperties config;

    @GetMapping("/start")
    public void start(@RequestParam String tenantSlug,
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
            // Google's own error (user denied consent, etc.)
            redirectError(res, "google_" + error);
            return;
        }
        if (code == null || state == null) {
            redirectError(res, "oauth_missing_params");
            return;
        }
        try {
            var result = google.handleCallback(code, state);
            // Tokens travel back to the SPA in the URL fragment — fragments
            // are not sent to the server in the next request, so they don't
            // appear in our access logs / referrers.
            String location = config.frontendCallback()
                    + "#accessToken=" + urlEncode(result.accessToken())
                    + "&refreshToken=" + urlEncode(result.refreshToken())
                    + "&tid=" + result.tenantId()
                    + "&uid=" + result.userId();
            res.sendRedirect(location);
        } catch (DomainException e) {
            log.info("OAuth callback failed: {} ({})", e.getCode(), e.getMessage());
            redirectError(res, e.getCode());
        } catch (Exception e) {
            log.error("OAuth callback unexpected error", e);
            redirectError(res, "oauth_unexpected");
        }
    }

    private void redirectError(HttpServletResponse res, String code) throws IOException {
        // Send the user to the login page with an error query they can render.
        String base = config.frontendCallback().replace("/login/oauth-callback", "/login");
        res.sendRedirect(base + "?oauth_error=" + urlEncode(code));
    }

    private static String urlEncode(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }
}
