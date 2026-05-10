package com.helloworlds.tms.identity.web;

import com.helloworlds.tms.identity.domain.Tenant;
import com.helloworlds.tms.identity.domain.TenantRepository;
import com.helloworlds.tms.platform.core.error.NotFoundException;
import com.helloworlds.tms.platform.core.tenant.TenantContext;
import com.helloworlds.tms.platform.security.rbac.PermissionEvaluator;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/tenant")
@RequiredArgsConstructor
public class TenantController {

    private final TenantRepository tenants;
    private final PermissionEvaluator perms;

    @GetMapping
    @Transactional(readOnly = true)
    public TenantDto current() {
        perms.requireAll("tenant", "read");
        Tenant t = tenants.findById(TenantContext.require())
                .orElseThrow(() -> new NotFoundException("tenant", TenantContext.require()));
        return TenantDto.from(t);
    }

    @PutMapping
    @Transactional
    public TenantDto update(@Valid @RequestBody UpdateTenantRequest req) {
        perms.requireAll("tenant", "update");
        Tenant t = tenants.findById(TenantContext.require())
                .orElseThrow(() -> new NotFoundException("tenant", TenantContext.require()));
        if (req.name() != null) t.setName(req.name());
        return TenantDto.from(t);
    }

    public record UpdateTenantRequest(@Size(max = 200) String name) {}

    public record TenantDto(UUID id, String slug, String name, String plan, String status) {
        static TenantDto from(Tenant t) {
            return new TenantDto(t.getId(), t.getSlug(), t.getName(), t.getPlan(), t.getStatus());
        }
    }
}
