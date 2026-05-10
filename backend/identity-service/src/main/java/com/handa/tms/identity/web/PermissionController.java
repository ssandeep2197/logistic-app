package com.handa.tms.identity.web;

import com.handa.tms.identity.domain.Permission;
import com.handa.tms.identity.domain.PermissionRepository;
import com.handa.tms.platform.security.rbac.PermissionEvaluator;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Read-only catalog of permissions in the system.  The frontend uses this to
 * render the role builder UI ("which permissions to attach to this role?").
 */
@RestController
@RequestMapping("/permissions")
@RequiredArgsConstructor
public class PermissionController {

    private final PermissionRepository permissions;
    private final PermissionEvaluator perms;

    @GetMapping
    @Transactional(readOnly = true)
    public List<PermissionDto> list() {
        perms.requireAll("role", "manage");   // Same gate as role mgmt.
        return permissions.findAll().stream()
                .map(p -> new PermissionDto(p.getId(), p.getResource(), p.getAction(), p.getScope(),
                        p.getDescription(), p.code()))
                .sorted((a, b) -> a.code().compareTo(b.code()))
                .toList();
    }

    public record PermissionDto(UUID id, String resource, String action, String scope, String description, String code) {}
}
