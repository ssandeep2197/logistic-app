package com.helloworlds.tms.identity.domain;

import com.helloworlds.tms.platform.core.persistence.TenantScopedEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "role")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Role extends TenantScopedEntity {

    @Column(nullable = false, length = 100)
    private String name;

    @Column(columnDefinition = "text")
    private String description;

    @Column(name = "is_system", nullable = false)
    private boolean systemRole;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "role_permission",
        joinColumns = @JoinColumn(name = "role_id"),
        inverseJoinColumns = @JoinColumn(name = "permission_id"))
    private Set<Permission> permissions = new HashSet<>();

    public static Role create(String name, String description) {
        Role r = new Role();
        r.setId(UUID.randomUUID());
        r.name = name;
        r.description = description;
        return r;
    }
}
