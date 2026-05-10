package com.helloworlds.tms.platform.security;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;

/**
 * Handlers for Spring Security exceptions.  Lives in {@code platform-lib-security}
 * (not {@code platform-lib-web}) so consumers of {@code platform-lib-web} who
 * don't use Spring Security don't pull a Spring Security transitive dep.
 * <p>
 * Ordered higher than {@link com.helloworlds.tms.platform.web.error.GlobalExceptionHandler}
 * so these specific handlers fire before the catch-all.
 */
@RestControllerAdvice
@Order(0)
public class SecurityExceptionHandler {

    private static final URI BASE = URI.create("https://errors.helloworlds.com/");

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ProblemDetail> handleAccessDenied(AccessDeniedException ex, HttpServletRequest req) {
        return problem(HttpStatus.FORBIDDEN, "forbidden", ex.getMessage(), req);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ProblemDetail> handleAuthentication(AuthenticationException ex, HttpServletRequest req) {
        return problem(HttpStatus.UNAUTHORIZED, "unauthorized", ex.getMessage(), req);
    }

    private ResponseEntity<ProblemDetail> problem(HttpStatus status, String code, String detail, HttpServletRequest req) {
        ProblemDetail p = ProblemDetail.forStatusAndDetail(status, detail);
        p.setType(BASE.resolve(code));
        p.setTitle(status.getReasonPhrase());
        p.setInstance(URI.create(req.getRequestURI()));
        p.setProperty("code", code);
        String requestId = MDC.get("requestId");
        if (requestId != null) p.setProperty("requestId", requestId);
        return ResponseEntity.status(status).contentType(MediaType.APPLICATION_PROBLEM_JSON).body(p);
    }
}
