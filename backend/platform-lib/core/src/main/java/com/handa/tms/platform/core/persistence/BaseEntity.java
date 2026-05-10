package com.handa.tms.platform.core.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

/**
 * Every persisted row gets a UUID v7 primary key and an optimistic-lock
 * version.  Tenant-scoped entities should extend {@link TenantScopedEntity}
 * instead of this — that one adds the {@code tenant_id} column and asserts
 * that {@link com.handa.tms.platform.core.tenant.TenantContext} is set on
 * insert (so writes can never escape their tenant).
 */
@MappedSuperclass
@Getter
@Setter
public abstract class BaseEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false, columnDefinition = "uuid")
    private UUID id;

    @Version
    @Column(name = "version", nullable = false)
    private long version;
}
