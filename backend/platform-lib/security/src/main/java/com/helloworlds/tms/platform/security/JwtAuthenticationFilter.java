package com.helloworlds.tms.platform.security;

import com.helloworlds.tms.platform.core.auth.AuthPrincipal;
import com.helloworlds.tms.platform.core.tenant.TenantContext;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Per-request filter: parse the bearer token, build {@link AuthPrincipal},
 * stamp it on the {@code SecurityContext}, populate {@link TenantContext} +
 * MDC, then invoke the rest of the chain.  Always clears the thread-locals
 * in finally so the worker thread is clean for the next request.
 * <p>
 * Bypasses validation for the public paths declared in
 * {@link com.helloworlds.tms.platform.security.SecurityProperties}.
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwt;
    private final SecurityProperties props;

    public JwtAuthenticationFilter(JwtService jwt, SecurityProperties props) {
        this.jwt = jwt;
        this.props = props;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {

        if (props.isPublic(req.getRequestURI())) {
            chain.doFilter(req, res);
            return;
        }

        String header = req.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            chain.doFilter(req, res);   // SecurityFilterChain will reject as 401.
            return;
        }

        try {
            AuthPrincipal principal = jwt.parseAccessToken(header.substring(7));
            TenantContext.set(principal.tenantId());
            MDC.put("tenantId", principal.tenantId().toString());
            MDC.put("userId", principal.userId().toString());

            // Granted authorities — Spring's @PreAuthorize uses these.  We only
            // expose ROLE_USER as a flat marker; fine-grained checks go through
            // PermissionEvaluator.has(...).
            UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(
                    principal, null, List.of(new SimpleGrantedAuthority("ROLE_USER")));
            SecurityContextHolder.getContext().setAuthentication(auth);

            chain.doFilter(req, res);
        } catch (JwtException e) {
            res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            res.setContentType("application/problem+json");
            res.getWriter().write("""
                {"type":"about:blank","title":"Unauthorized","status":401,"code":"invalid_token","detail":"%s"}
                """.formatted(e.getMessage().replace("\"", "'")));
        } finally {
            TenantContext.clear();
            MDC.remove("tenantId");
            MDC.remove("userId");
            SecurityContextHolder.clearContext();
        }
    }
}
