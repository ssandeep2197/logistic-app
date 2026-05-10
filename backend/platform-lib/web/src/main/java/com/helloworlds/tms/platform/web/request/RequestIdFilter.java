package com.helloworlds.tms.platform.web.request;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Stamps every request with a request id and propagates it through the MDC
 * (so all log lines carry it) and the response header (so clients can quote
 * it in support tickets).  If the caller already supplied {@code X-Request-Id},
 * we honor it — that's how a request id stays the same across the gateway and
 * downstream services.
 */
public class RequestIdFilter extends OncePerRequestFilter {

    public static final String HEADER = "X-Request-Id";

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        String id = req.getHeader(HEADER);
        if (id == null || id.isBlank()) id = UUID.randomUUID().toString();
        MDC.put("requestId", id);
        res.setHeader(HEADER, id);
        try {
            chain.doFilter(req, res);
        } finally {
            MDC.remove("requestId");
        }
    }
}
