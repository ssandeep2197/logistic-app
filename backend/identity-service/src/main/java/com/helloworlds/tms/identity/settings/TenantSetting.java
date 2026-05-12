package com.helloworlds.tms.identity.settings;

import com.helloworlds.tms.platform.core.persistence.TenantScopedEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/**
 * Per-tenant override of a {@link PlatformSetting} key.  Effective value of
 * a flag = platform value AND tenant value (when both are booleans).  The
 * platform-side flag is the ceiling; tenants opt in or out under it.
 * <p>
 * Inherits id, version, tenant_id, created_at, updated_at from
 * {@link TenantScopedEntity}.  RLS-protected by the policy in migration
 * 0008 — a tenant can only read or modify its own settings.
 */
@Entity
@Table(name = "tenant_setting",
       uniqueConstraints = @UniqueConstraint(columnNames = {"tenant_id", "key"}))
@Getter
@Setter
@NoArgsConstructor
public class TenantSetting extends TenantScopedEntity {

    @Column(nullable = false, length = 80)
    private String key;

    @Column(nullable = false, columnDefinition = "text")
    private String value;

    /** UUID of the tenant-admin user who last touched this row. */
    @Column(name = "updated_by", columnDefinition = "uuid")
    private UUID updatedBy;

    public static TenantSetting of(String key, String value, UUID by) {
        TenantSetting s = new TenantSetting();
        s.setId(UUID.randomUUID());
        s.key = key;
        s.value = value;
        s.updatedBy = by;
        return s;
    }
}
