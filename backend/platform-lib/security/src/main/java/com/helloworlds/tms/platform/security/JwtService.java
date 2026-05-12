package com.helloworlds.tms.platform.security;

import com.helloworlds.tms.platform.core.auth.AuthPrincipal;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;

/**
 * Issues and parses JWTs.  All services use the same {@code parse} path; only
 * identity-service calls {@code issueAccessToken} / {@code issueRefreshToken}.
 * <p>
 * Permissions are pre-flattened in the {@code "perm"} claim so receiving
 * services can authorize without an RPC back to identity.  Tradeoff: when a
 * user's permissions change, they only take effect on the next token refresh
 * (≤ 15 min by default).  Acceptable for a TMS; revisit if it becomes an issue.
 */
public class JwtService {

    private final SecretKey key;
    private final JwtProperties props;
    private final JwtParser parser;

    public JwtService(JwtProperties props) {
        this.props = props;
        this.key = Keys.hmacShaKeyFor(props.signingKey().getBytes(StandardCharsets.UTF_8));
        this.parser = Jwts.parser()
                .verifyWith(key)
                .requireIssuer(props.issuer())
                .build();
    }

    public String issueAccessToken(AuthPrincipal principal) {
        Instant now = Instant.now();
        return Jwts.builder()
                .issuer(props.issuer())
                .subject(principal.userId().toString())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(props.accessTokenTtl())))
                .claim("tid", principal.tenantId().toString())
                .claim("eml", principal.email())
                .claim("perm", new ArrayList<>(principal.permissions()))
                .claim("brn", principal.branchIds().stream().map(UUID::toString).toList())
                .claim("typ", "access")
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }

    public String issueRefreshToken(UUID userId, UUID tenantId) {
        Instant now = Instant.now();
        return Jwts.builder()
                .issuer(props.issuer())
                .subject(userId.toString())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(props.refreshTokenTtl())))
                .claim("tid", tenantId.toString())
                .claim("typ", "refresh")
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }

    /** Parses and validates an access token; throws {@link JwtException} on failure. */
    public AuthPrincipal parseAccessToken(String token) {
        Jws<Claims> parsed = parser.parseSignedClaims(token);
        Claims c = parsed.getPayload();
        if (!"access".equals(c.get("typ", String.class))) {
            throw new JwtException("Not an access token");
        }
        @SuppressWarnings("unchecked")
        List<String> perms = c.get("perm", List.class);
        @SuppressWarnings("unchecked")
        List<String> branches = c.get("brn", List.class);
        return new AuthPrincipal(
                UUID.fromString(c.getSubject()),
                UUID.fromString(c.get("tid", String.class)),
                c.get("eml", String.class),
                perms == null ? Set.of() : new HashSet<>(perms),
                branches == null ? Set.of()
                                 : branches.stream().map(UUID::fromString).collect(java.util.stream.Collectors.toSet())
        );
    }

    /** Returns the user id and tenant id from a refresh token; nothing else is trustworthy. */
    public RefreshClaims parseRefreshToken(String token) {
        Jws<Claims> parsed = parser.parseSignedClaims(token);
        Claims c = parsed.getPayload();
        if (!"refresh".equals(c.get("typ", String.class))) {
            throw new JwtException("Not a refresh token");
        }
        return new RefreshClaims(
                UUID.fromString(c.getSubject()),
                UUID.fromString(c.get("tid", String.class)));
    }

    public record RefreshClaims(UUID userId, UUID tenantId) {}

    /**
     * Issues a short-lived signed JWT that carries arbitrary string claims.
     * Used for OAuth state, password-reset tokens, etc. — anything that needs
     * to round-trip through the user's browser without us trusting it on the
     * way back.  The signature gives us tamper-resistance; the {@code typ}
     * claim segregates state tokens from access/refresh tokens.
     */
    public String issueStateToken(String purpose, java.util.Map<String, String> claims, java.time.Duration ttl) {
        Instant now = Instant.now();
        JwtBuilder b = Jwts.builder()
                .issuer(props.issuer())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(ttl)))
                .claim("typ", "state")
                .claim("pur", purpose);
        claims.forEach(b::claim);
        return b.signWith(key, Jwts.SIG.HS256).compact();
    }

    /** Parses a state token, verifying signature, issuer, expiry, and purpose. */
    public java.util.Map<String, String> parseStateToken(String token, String expectedPurpose) {
        Claims c = parser.parseSignedClaims(token).getPayload();
        if (!"state".equals(c.get("typ", String.class))) {
            throw new JwtException("Not a state token");
        }
        if (!expectedPurpose.equals(c.get("pur", String.class))) {
            throw new JwtException("State token purpose mismatch");
        }
        java.util.Map<String, String> out = new java.util.HashMap<>();
        for (var e : c.entrySet()) {
            if (e.getValue() instanceof String s) out.put(e.getKey(), s);
        }
        return out;
    }
}
