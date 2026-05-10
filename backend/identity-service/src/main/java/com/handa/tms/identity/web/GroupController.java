package com.handa.tms.identity.web;

import com.handa.tms.identity.domain.AppGroup;
import com.handa.tms.identity.domain.AppGroupRepository;
import com.handa.tms.identity.domain.AppUser;
import com.handa.tms.identity.domain.AppUserRepository;
import com.handa.tms.identity.domain.Role;
import com.handa.tms.identity.domain.RoleRepository;
import com.handa.tms.platform.core.error.NotFoundException;
import com.handa.tms.platform.security.rbac.PermissionEvaluator;
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

@RestController
@RequestMapping("/groups")
@RequiredArgsConstructor
public class GroupController {

    private final AppGroupRepository groups;
    private final RoleRepository roles;
    private final AppUserRepository users;
    private final PermissionEvaluator perms;

    @GetMapping
    @Transactional(readOnly = true)
    public List<GroupSummary> list() {
        perms.requireAll("group", "manage");
        return groups.findAll().stream().map(GroupSummary::from).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public GroupSummary create(@Valid @RequestBody GroupRequest req) {
        perms.requireAll("group", "manage");
        AppGroup g = AppGroup.create(req.name(), req.description());
        attachRoles(g, req.roleIds());
        return GroupSummary.from(groups.save(g));
    }

    @PutMapping("/{id}")
    @Transactional
    public GroupSummary update(@PathVariable UUID id, @Valid @RequestBody GroupRequest req) {
        perms.requireAll("group", "manage");
        AppGroup g = groups.findById(id).orElseThrow(() -> new NotFoundException("group", id));
        g.setName(req.name());
        g.setDescription(req.description());
        g.getRoles().clear();
        attachRoles(g, req.roleIds());
        return GroupSummary.from(g);
    }

    @PostMapping("/{id}/users/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Transactional
    public void addUser(@PathVariable UUID id, @PathVariable UUID userId) {
        perms.requireAll("group", "manage");
        AppGroup g = groups.findById(id).orElseThrow(() -> new NotFoundException("group", id));
        AppUser u = users.findById(userId).orElseThrow(() -> new NotFoundException("user", userId));
        u.getGroups().add(g);
    }

    @DeleteMapping("/{id}/users/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Transactional
    public void removeUser(@PathVariable UUID id, @PathVariable UUID userId) {
        perms.requireAll("group", "manage");
        AppGroup g = groups.findById(id).orElseThrow(() -> new NotFoundException("group", id));
        AppUser u = users.findById(userId).orElseThrow(() -> new NotFoundException("user", userId));
        u.getGroups().remove(g);
    }

    private void attachRoles(AppGroup g, Set<UUID> ids) {
        if (ids == null) return;
        for (UUID rid : ids) {
            Role r = roles.findById(rid).orElseThrow(() -> new NotFoundException("role", rid));
            g.getRoles().add(r);
        }
    }

    public record GroupRequest(
            @NotBlank @Size(max = 100) String name,
            @Size(max = 1000) String description,
            Set<UUID> roleIds) {}

    public record GroupSummary(UUID id, String name, String description, List<String> roles) {
        static GroupSummary from(AppGroup g) {
            return new GroupSummary(g.getId(), g.getName(), g.getDescription(),
                    g.getRoles().stream().map(Role::getName).sorted().toList());
        }
    }
}
