package com.helloworlds.tms.identity.settings;

import com.helloworlds.tms.platform.core.auth.AuthPrincipal;
import com.helloworlds.tms.platform.core.error.ForbiddenException;
import com.helloworlds.tms.platform.security.rbac.PermissionEvaluator;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Two endpoint trees — one for platform-owner settings, one for tenant-admin
 * settings.  Both POST/PUT shapes the same body so the frontend can reuse
 * the toggle component.
 *
 * <pre>
 *   GET  /platform/settings              owner → all platform key/value
 *   PUT  /platform/settings/{key}        owner → set one
 *
 *   GET  /tenant/settings                tenant admin → all tenant key/value + cascade
 *   PUT  /tenant/settings/{key}          tenant admin → set one (cascade-gated)
 * </pre>
 *
 * Defense in depth: platform routes require {@code principal.platformAdmin};
 * tenant routes require {@code tenant:update:all} like other tenant-admin
 * operations.
 */
@RestController
@RequiredArgsConstructor
public class SettingsController {

    private final SettingsService settings;
    private final PermissionEvaluator perms;

    // ── Platform (owner-only) ────────────────────────────────────────────

    @GetMapping("/platform/settings")
    public Map<String, String> readPlatform(@AuthenticationPrincipal AuthPrincipal me) {
        requirePlatformAdmin(me);
        return settings.readAllPlatform();
    }

    @PutMapping("/platform/settings/{key}")
    public Map<String, String> writePlatform(@PathVariable String key,
                                              @Valid @RequestBody SettingBody body,
                                              @AuthenticationPrincipal AuthPrincipal me) {
        requirePlatformAdmin(me);
        SettingsService.Key k = SettingsService.Key.byName(key);
        settings.writePlatform(k, body.value(), me.userId());
        return settings.readAllPlatform();
    }

    // ── Tenant (tenant-admin) ────────────────────────────────────────────

    /**
     * Returns a map per setting key with both the tenant override (if any)
     * and the platform value the tenant override is gated against.  Lets the
     * frontend show "can't enable — owner disabled" UX with a single fetch.
     */
    @GetMapping("/tenant/settings")
    public TenantSettingsView readTenant(@AuthenticationPrincipal AuthPrincipal me) {
        // No permission gate on READ — any authed user sees their tenant's
        // settings.  WRITE is gated below.
        return new TenantSettingsView(
            settings.readAllPlatform(),
            settings.readAllTenant()
        );
    }

    @PutMapping("/tenant/settings/{key}")
    public TenantSettingsView writeTenant(@PathVariable String key,
                                           @Valid @RequestBody SettingBody body,
                                           @AuthenticationPrincipal AuthPrincipal me) {
        perms.requireAll("tenant", "update");
        SettingsService.Key k = SettingsService.Key.byName(key);
        settings.writeTenant(k, body.value(), me.userId());
        return new TenantSettingsView(
            settings.readAllPlatform(),
            settings.readAllTenant()
        );
    }

    private void requirePlatformAdmin(AuthPrincipal me) {
        if (me == null || !me.platformAdmin()) {
            throw new ForbiddenException("platform admin only");
        }
    }

    // ── DTOs ──────────────────────────────────────────────────────────────

    public record SettingBody(@NotBlank @Pattern(regexp = "true|false|\\d+|.{0,200}") String value) {}

    /**
     * What the tenant admin's settings page renders from.  {@code platform}
     * tells it which toggles the platform owner has globally enabled (so
     * the UI can grey out the override when the parent is off).
     */
    public record TenantSettingsView(
            Map<String, String> platform,
            Map<String, String> tenant) {}
}
