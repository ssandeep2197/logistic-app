package com.handa.tms.platform.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Configures JWT signing/validation per service.
 * <p>
 * identity-service is the only service that signs tokens — it owns the secret.
 * Every other service only validates, so they need just the secret (HS256) or
 * the public key (RS256) used to verify the signature.  Start with HS256 +
 * shared secret for v1; rotate to RS256 + JWKS once the service mesh exists.
 */
@ConfigurationProperties("tms.security.jwt")
public record JwtProperties(
        String signingKey,
        Duration accessTokenTtl,
        Duration refreshTokenTtl,
        String issuer
) {
    public JwtProperties {
        if (signingKey == null || signingKey.length() < 32) {
            throw new IllegalStateException(
                "tms.security.jwt.signing-key must be set and at least 32 bytes. "
                + "Generate one with: openssl rand -hex 32");
        }
        if (accessTokenTtl == null) accessTokenTtl = Duration.ofMinutes(15);
        if (refreshTokenTtl == null) refreshTokenTtl = Duration.ofDays(30);
        if (issuer == null) issuer = "tms-identity";
    }
}
