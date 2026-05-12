package com.helloworlds.tms.identity.settings;

import com.helloworlds.tms.platform.core.error.DomainException;
import com.helloworlds.tms.platform.core.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * One-stop shop for reading and writing platform-level and tenant-level
 * settings.  Cascading lookup ({@link #effective(String, boolean)}) returns
 * the AND of the platform value and the current tenant's override — used by
 * places like the backup CronJob that need the final effective answer.
 * <p>
 * Validation rules enforced here:
 *   - Only known keys can be set (whitelisted) → callers can't pollute the
 *     table with typo'd keys.
 *   - Tenant override can only be set when the platform value permits it
 *     (e.g. tenant cannot enable a feature the platform has disabled).
 */
@Service
@RequiredArgsConstructor
public class SettingsService {

    /** Single source of truth for known setting keys + their defaults. */
    public enum Key {
        BACKUP_ENABLED("backup.enabled", true, false);

        public final String name;
        /** Platform-level default if no row exists. */
        public final boolean platformDefault;
        /** Tenant-level default if no row exists — only meaningful when boolean. */
        public final boolean tenantDefault;

        Key(String name, boolean platformDefault, boolean tenantDefault) {
            this.name = name;
            this.platformDefault = platformDefault;
            this.tenantDefault = tenantDefault;
        }

        public static Key byName(String name) {
            for (Key k : values()) if (k.name.equals(name)) return k;
            throw new DomainException("unknown_setting", 400, "Unknown setting: " + name);
        }
    }

    private final PlatformSettingRepository platform;
    private final TenantSettingRepository tenant;

    // ── Platform-level operations ─────────────────────────────────────────

    @Transactional(readOnly = true)
    public Map<String, String> readAllPlatform() {
        return platform.findAll().stream()
                .collect(Collectors.toMap(PlatformSetting::getKey, PlatformSetting::getValue));
    }

    @Transactional(readOnly = true)
    public boolean readPlatformBool(Key key) {
        return platform.findById(key.name)
                .map(s -> Boolean.parseBoolean(s.getValue()))
                .orElse(key.platformDefault);
    }

    @Transactional
    public void writePlatform(Key key, String value, UUID by) {
        platform.findById(key.name).ifPresentOrElse(
            existing -> {
                existing.setValue(value);
                existing.setUpdatedBy(by);
            },
            () -> platform.save(PlatformSetting.of(key.name, value, by)));
    }

    // ── Tenant-level operations (run inside TenantContext) ────────────────

    @Transactional(readOnly = true)
    public Map<String, String> readAllTenant() {
        return tenant.findAllByTenantId(TenantContext.require()).stream()
                .collect(Collectors.toMap(TenantSetting::getKey, TenantSetting::getValue));
    }

    @Transactional(readOnly = true)
    public boolean readTenantBool(Key key) {
        return tenant.findByKey(key.name)
                .map(s -> Boolean.parseBoolean(s.getValue()))
                .orElse(key.tenantDefault);
    }

    /**
     * Cascading effective value.  Only meaningful for boolean settings.
     * Used by the backup CronJob (platform side) and could be used by
     * service code that needs to know if a tenant-specific feature is
     * actually on (e.g. "should we charge $X to this tenant for backups?").
     */
    @Transactional(readOnly = true)
    public boolean effectiveBool(Key key, UUID tenantId) {
        if (!readPlatformBool(key)) return false;
        return tenant.findByKey(key.name)
                .filter(s -> s.getTenantId().equals(tenantId))
                .map(s -> Boolean.parseBoolean(s.getValue()))
                .orElse(key.tenantDefault);
    }

    @Transactional
    public void writeTenant(Key key, String value, UUID by) {
        // Cascade guard: if the platform value is false for a boolean flag,
        // tenants can't override.  Frontend already greys the toggle out;
        // server enforces it as defense in depth.
        if (isBoolean(value) && Boolean.parseBoolean(value) && !readPlatformBool(key)) {
            throw new DomainException("setting_blocked_by_platform", 409,
                    "Platform owner has disabled '" + key.name + "'; cannot enable per-tenant");
        }

        tenant.findByKey(key.name).ifPresentOrElse(
            existing -> {
                existing.setValue(value);
                existing.setUpdatedBy(by);
            },
            () -> tenant.save(TenantSetting.of(key.name, value, by)));
    }

    private boolean isBoolean(String v) { return "true".equals(v) || "false".equals(v); }
}
