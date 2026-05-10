package com.handa.tms.platform.core.persistence;

import com.handa.tms.platform.core.tenant.TenantContext;
import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * Base class for any entity that is per-tenant data.  Stamps {@code tenant_id}
 * from {@link TenantContext} on insert and refuses to persist if no tenant is
 * set — that almost always means a bug (a job missing {@code TenantContext.runAs},
 * a controller bypassing the security filter).
 */
@MappedSuperclass
@Getter
@Setter
public abstract class TenantScopedEntity extends BaseEntity {

    @Column(name = "tenant_id", nullable = false, updatable = false, columnDefinition = "uuid")
    private UUID tenantId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onPersist() {
        if (tenantId == null) {
            // Will throw if no tenant — intentional.
            tenantId = TenantContext.require();
        }
        Instant now = Instant.now();
        if (createdAt == null) createdAt = now;
        updatedAt = now;
    }
}
