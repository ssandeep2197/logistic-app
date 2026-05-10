package com.handa.tms.platform.security;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.AntPathMatcher;

import java.util.List;
import java.util.Set;

/**
 * Per-service overrides for security defaults.  Each service can declare
 * additional public paths (e.g. identity-service exposes /auth/login).
 */
@ConfigurationProperties("tms.security")
public record SecurityProperties(
        List<String> publicPaths,
        boolean enableRlsGuc
) {
    private static final AntPathMatcher MATCHER = new AntPathMatcher();
    private static final Set<String> ALWAYS_PUBLIC = Set.of(
        "/actuator/health", "/actuator/health/**",
        "/actuator/info",
        "/v3/api-docs", "/v3/api-docs/**",
        "/swagger-ui/**", "/swagger-ui.html"
    );

    public SecurityProperties {
        if (publicPaths == null) publicPaths = List.of();
    }

    public boolean isPublic(String path) {
        for (String p : ALWAYS_PUBLIC) if (MATCHER.match(p, path)) return true;
        for (String p : publicPaths)   if (MATCHER.match(p, path)) return true;
        return false;
    }
}
