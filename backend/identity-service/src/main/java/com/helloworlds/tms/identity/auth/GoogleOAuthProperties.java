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
 *   <li>{@code OAUTH_FRONTEND_BASE_URL} — public origin of the SPA, e.g.
 *       {@code https://tms.helloworlds.co.in}.  Used to build the three
 *       browser-side destination URLs (success, error, workspace setup).</li>
 * </ul>
 * If {@code clientId} is blank, the OAuth controller refuses the flow so we
 * can deploy the code before the Google credentials exist.
 */
@ConfigurationProperties(prefix = "tms.oauth.google")
public record GoogleOAuthProperties(
        String clientId,
        String clientSecret,
        String redirectUri,
        String frontendBaseUrl) {

    public boolean isConfigured() {
        return clientId != null && !clientId.isBlank()
            && clientSecret != null && !clientSecret.isBlank();
    }

    /** Where to land the browser after a successful sign-in (tokens in URL fragment). */
    public String successUrl()   { return frontendBaseUrl + "/login/oauth-callback"; }

    /** Where to land the browser when something failed (?oauth_error=<code>). */
    public String errorUrl()     { return frontendBaseUrl + "/login"; }

    /**
     * Where to send the browser after Google identifies a new user that does
     * NOT yet have a workspace.  Page reads the pendingSignup JWT from the
     * URL fragment and shows the slug-entry form.
     */
    public String workspaceSetupUrl() { return frontendBaseUrl + "/signup/workspace"; }
}
