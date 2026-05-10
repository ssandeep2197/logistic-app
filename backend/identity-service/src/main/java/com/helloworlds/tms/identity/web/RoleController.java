package com.helloworlds.tms.identity.web;

import com.helloworlds.tms.identity.domain.Permission;
import com.helloworlds.tms.identity.domain.PermissionRepository;
import com.helloworlds.tms.identity.domain.Role;
import com.helloworlds.tms.identity.domain.RoleRepository;
import com.helloworlds.tms.platform.core.error.NotFoundException;
import com.helloworlds.tms.platform.security.rbac.PermissionEvaluator;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Roles + their permissions.  A role is a named bag of permissions; admins
 * compose roles, then attach them to groups (or directly to users).
 */
@RestController
@RequestMapping("/roles")
@RequiredArgsConstructor
public class RoleController {

    private final RoleRepository roles;
    private final PermissionRepository permissions;
    private final PermissionEvaluator perms;

    @GetMapping
    @Transactional(readOnly = true)
    public List<RoleSummary> list() {
        perms.requireAll("role", "manage");
        return roles.findAll().stream().map(RoleSummary::from).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public RoleSummary create(@Valid @RequestBody RoleRequest req) {
        perms.requireAll("role", "manage");
        Role r = Role.create(req.name(), req.description());
        attachPermissions(r, req.permissionIds());
        return RoleSummary.from(roles.save(r));
    }

    @PutMapping("/{id}")
    @Transactional
    public RoleSummary update(@PathVariable UUID id, @Valid @RequestBody RoleRequest req) {
        perms.requireAll("role", "manage");
        Role r = roles.findById(id).orElseThrow(() -> new NotFoundException("role", id));
        if (r.isSystemRole()) {
            throw new com.helloworlds.tms.platform.core.error.DomainException("system_role_immutable", 409,
                    "System roles cannot be edited");
        }
        r.setName(req.name());
        r.setDescription(req.description());
        r.getPermissions().clear();
        attachPermissions(r, req.permissionIds());
        return RoleSummary.from(r);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Transactional
    public void delete(@PathVariable UUID id) {
        perms.requireAll("role", "manage");
        Role r = roles.findById(id).orElseThrow(() -> new NotFoundException("role", id));
        if (r.isSystemRole()) {
            throw new com.helloworlds.tms.platform.core.error.DomainException("system_role_immutable", 409,
                    "System roles cannot be deleted");
        }
        roles.delete(r);
    }

    private void attachPermissions(Role r, Set<UUID> ids) {
        if (ids == null) return;
        for (UUID pid : ids) {
            Permission p = permissions.findById(pid).orElseThrow(() -> new NotFoundException("permission", pid));
            r.getPermissions().add(p);
        }
    }

    public record RoleRequest(
            @NotBlank @Size(max = 100) String name,
            @Size(max = 1000) String description,
            Set<UUID> permissionIds) {}

    public record RoleSummary(UUID id, String name, String description, boolean systemRole, List<String> permissions) {
        static RoleSummary from(Role r) {
            return new RoleSummary(r.getId(), r.getName(), r.getDescription(), r.isSystemRole(),
                    r.getPermissions().stream().map(Permission::code).sorted().toList());
        }
    }
}
