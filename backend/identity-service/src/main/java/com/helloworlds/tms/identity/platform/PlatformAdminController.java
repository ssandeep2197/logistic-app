package com.helloworlds.tms.identity.platform;

import com.helloworlds.tms.platform.core.auth.AuthPrincipal;
import com.helloworlds.tms.platform.core.error.ForbiddenException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Cross-tenant read endpoints for the platform owner.  Every method gates
 * on {@link AuthPrincipal#platformAdmin()} — non-owners get a 403 even if
 * they have every tenant-level permission.
 */
@RestController
@RequestMapping("/platform")
@RequiredArgsConstructor
public class PlatformAdminController {

    private final PlatformAdminService svc;

    @GetMapping("/stats")
    public PlatformAdminService.Stats stats(@AuthenticationPrincipal AuthPrincipal me) {
        gate(me);
        return svc.stats();
    }

    @GetMapping("/tenants")
    public List<PlatformAdminService.TenantRow> tenants(@AuthenticationPrincipal AuthPrincipal me,
                                                         @RequestParam(defaultValue = "20") int limit) {
        gate(me);
        return svc.recentTenants(Math.min(Math.max(limit, 1), 200));
    }

    @GetMapping("/users")
    public List<PlatformAdminService.UserRow> users(@AuthenticationPrincipal AuthPrincipal me,
                                                     @RequestParam(defaultValue = "20") int limit) {
        gate(me);
        return svc.recentUsers(Math.min(Math.max(limit, 1), 200));
    }

    private void gate(AuthPrincipal me) {
        if (me == null || !me.platformAdmin()) {
            throw new ForbiddenException("platform admin only");
        }
    }
}
