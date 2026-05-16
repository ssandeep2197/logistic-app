package com.helloworlds.tms.operations.accounting;

import com.helloworlds.tms.platform.core.persistence.BaseEntity;
import com.helloworlds.tms.platform.core.tenant.TenantContext;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * One row per sensitive action on a {@link BankAccount}: reveal, create,
 * update, archive.  No payload — just who/when/which.  Tenant-scoped so the
 * platform owner can roll up across tenants but tenants only see their own.
 * <p>
 * Not a {@link com.helloworlds.tms.platform.core.persistence.TenantScopedEntity}
 * because that base class auto-stamps {@code updated_at} on every persist;
 * an audit row is immutable, so we stamp {@code at} ourselves and skip the
 * machinery.
 */
@Entity
@Table(name = "bank_account_audit")
@Getter
@Setter
@NoArgsConstructor
public class BankAccountAudit extends BaseEntity {

    @Column(name = "tenant_id", nullable = false, updatable = false, columnDefinition = "uuid")
    private UUID tenantId;

    @Column(name = "bank_account_id", nullable = false, columnDefinition = "uuid")
    private UUID bankAccountId;

    /** {@code reveal | create | update | archive}. */
    @Column(nullable = false, length = 20)
    private String action;

    @Column(name = "actor_user_id", columnDefinition = "uuid")
    private UUID actorUserId;

    @Column(name = "actor_ip", length = 45)
    private String actorIp;

    @Column(name = "at", nullable = false, updatable = false)
    private Instant at;

    @PrePersist
    void onPersist() {
        if (tenantId == null) tenantId = TenantContext.require();
        if (at == null) at = Instant.now();
    }
}
