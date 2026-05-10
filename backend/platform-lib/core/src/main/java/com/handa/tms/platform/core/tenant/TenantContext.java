package com.handa.tms.platform.core.tenant;

import java.util.UUID;

/**
 * Holds the current tenant for the duration of a request, on a thread-local.
 * <p>
 * The {@code TenantContextFilter} in {@code platform-lib-security} populates
 * this from the JWT and clears it in a finally block.  JPA, Kafka producers,
 * and outbox writers read from it to stamp the right {@code tenant_id}.
 * <p>
 * Postgres RLS still enforces the boundary at the DB layer, so a missing
 * filter call cannot leak data — but writes that forget to set the tenant
 * will fail loudly rather than silently corrupting another tenant's data.
 */
public final class TenantContext {

    private static final ThreadLocal<UUID> CURRENT = new ThreadLocal<>();

    private TenantContext() {}

    public static void set(UUID tenantId) {
        if (tenantId == null) {
            throw new IllegalArgumentException("tenantId must not be null");
        }
        CURRENT.set(tenantId);
    }

    public static UUID require() {
        UUID t = CURRENT.get();
        if (t == null) {
            throw new IllegalStateException(
                "No tenant in context. The request reached a tenant-scoped operation "
                + "without a TenantContextFilter populating it. Check the security chain.");
        }
        return t;
    }

    public static UUID currentOrNull() {
        return CURRENT.get();
    }

    public static void clear() {
        CURRENT.remove();
    }

    /** For background jobs / Kafka consumers that must process a specific tenant. */
    public static <T> T runAs(UUID tenantId, java.util.function.Supplier<T> work) {
        UUID prev = CURRENT.get();
        CURRENT.set(tenantId);
        try {
            return work.get();
        } finally {
            if (prev == null) {
                CURRENT.remove();
            } else {
                CURRENT.set(prev);
            }
        }
    }
}
