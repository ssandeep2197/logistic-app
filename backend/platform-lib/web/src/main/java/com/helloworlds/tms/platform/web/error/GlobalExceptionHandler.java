package com.helloworlds.tms.platform.web.error;

import com.helloworlds.tms.platform.core.error.DomainException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.net.URI;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Maps exceptions to RFC 7807 {@code application/problem+json} responses.
 * Every service that depends on {@code platform-lib-web} gets the same shape
 * automatically; clients can rely on {@code code} for machine-readable
 * dispatch and on {@code requestId} for support correlation.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private static final URI BASE = URI.create("https://errors.helloworlds.com/");

    @ExceptionHandler(DomainException.class)
    public ResponseEntity<ProblemDetail> handleDomain(DomainException ex, HttpServletRequest req) {
        ProblemDetail body = problem(HttpStatus.valueOf(ex.getStatus()), ex.getCode(), ex.getMessage(), req);
        return ResponseEntity.status(ex.getStatus()).contentType(MediaType.APPLICATION_PROBLEM_JSON).body(body);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest req) {
        Map<String, String> fields = ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(
                        f -> f.getField(),
                        f -> f.getDefaultMessage() == null ? "invalid" : f.getDefaultMessage(),
                        (a, b) -> a));
        ProblemDetail body = problem(HttpStatus.UNPROCESSABLE_ENTITY, "validation_failed",
                "One or more fields are invalid", req);
        body.setProperty("fields", fields);
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON).body(body);
    }

    // Spring Security's AccessDeniedException + AuthenticationException are
    // handled in {@code SecurityExceptionHandler} in platform-lib-security so
    // this module stays free of a Spring Security dependency.

    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ProblemDetail> handleNotFound(NoHandlerFoundException ex, HttpServletRequest req) {
        ProblemDetail body = problem(HttpStatus.NOT_FOUND, "not_found", "No route for " + req.getRequestURI(), req);
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON).body(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleAll(Exception ex, HttpServletRequest req) {
        log.error("unhandled exception on {} {}", req.getMethod(), req.getRequestURI(), ex);
        ProblemDetail body = problem(HttpStatus.INTERNAL_SERVER_ERROR, "internal_error",
                "An unexpected error occurred. Please contact support with the requestId.", req);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON).body(body);
    }

    private ProblemDetail problem(HttpStatus status, String code, String detail, HttpServletRequest req) {
        ProblemDetail p = ProblemDetail.forStatusAndDetail(status, detail);
        p.setType(BASE.resolve(code));
        p.setTitle(status.getReasonPhrase());
        p.setInstance(URI.create(req.getRequestURI()));
        p.setProperty("code", code);
        String requestId = MDC.get("requestId");
        if (requestId != null) p.setProperty("requestId", requestId);
        return p;
    }
}
