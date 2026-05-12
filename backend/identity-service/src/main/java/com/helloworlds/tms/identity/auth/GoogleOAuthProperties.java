package com.helloworlds.tms.identity.auth;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Google OAuth client config.  Populated from env vars via application.yml:
 * <ul>
 *   <li>{@code GOOGLE_CLIENT_ID}     — public, looks like {@code 1234.apps.googleusercontent.com}</li>
 *   <li>{@code GOOGLE_CLIENT_SECRET} — SECRET, comes from a k8s Secret mount</li>
 *   <li>{@code GOOGLE_REDIRECT_URI}  — must EXACTLY match what's registered in
 *       Google Cloud Console.  Includes the full URL, e.g.
 *       {@code https://tms.helloworlds.co.in/api/identity/auth/oauth/google/callback}.</li>
 *   <li>{@code OAUTH_FRONTEND_CALLBACK} — where to bounce the browser after
 *       a successful exchange, e.g.
 *       {@code https://tms.helloworlds.co.in/login/oauth-callback}.</li>
 * </ul>
 * If {@code clientId} is blank, the OAuth controller returns 503 so we can
 * deploy the code before the Google credentials exist.
 */
@ConfigurationProperties(prefix = "tms.oauth.google")
public record GoogleOAuthProperties(
        String clientId,
        String clientSecret,
        String redirectUri,
        String frontendCallback) {

    public boolean isConfigured() {
        return clientId != null && !clientId.isBlank()
            && clientSecret != null && !clientSecret.isBlank();
    }
}
