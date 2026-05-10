package com.handa.tms.identity.domain;

import com.handa.tms.platform.core.persistence.TenantScopedEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "app_group")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AppGroup extends TenantScopedEntity {

    @Column(nullable = false, length = 100)
    private String name;

    @Column(columnDefinition = "text")
    private String description;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "group_role",
        joinColumns = @JoinColumn(name = "group_id"),
        inverseJoinColumns = @JoinColumn(name = "role_id"))
    private Set<Role> roles = new HashSet<>();

    public static AppGroup create(String name, String description) {
        AppGroup g = new AppGroup();
        g.setId(UUID.randomUUID());
        g.name = name;
        g.description = description;
        return g;
    }
}
