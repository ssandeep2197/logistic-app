package com.helloworlds.tms.identity.settings;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TenantSettingRepository extends JpaRepository<TenantSetting, UUID> {

    /** Lookup by (current tenant via RLS) + key. */
    Optional<TenantSetting> findByKey(String key);

    /** All settings for the current tenant — used by /tenant/settings. */
    List<TenantSetting> findAllByTenantId(UUID tenantId);
}
