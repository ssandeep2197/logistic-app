package com.helloworlds.tms.identity.domain;

import com.helloworlds.tms.platform.core.persistence.TenantScopedEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "app_user")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AppUser extends TenantScopedEntity {

    @Column(nullable = false, length = 320)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 200)
    private String passwordHash;

    @Column(name = "full_name", length = 200)
    private String fullName;

    /** {@code active}, {@code invited}, {@code suspended}, {@code closed}. */
    @Column(nullable = false, length = 20)
    private String status = "active";

    @Column(name = "last_login_at")
    private Instant lastLoginAt;

    /** True for the operator(s) of the SaaS itself.  Set via a direct SQL
     *  UPDATE — not exposed in any API endpoint.  See migration 0007. */
    @Column(name = "is_platform_admin", nullable = false)
    private boolean platformAdmin = false;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "user_group",
        joinColumns = @JoinColumn(name = "user_id"),
        inverseJoinColumns = @JoinColumn(name = "group_id"))
    private Set<AppGroup> groups = new HashSet<>();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "user_role",
        joinColumns = @JoinColumn(name = "user_id"),
        inverseJoinColumns = @JoinColumn(name = "role_id"))
    private Set<Role> directRoles = new HashSet<>();

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "user_branch", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "branch_id")
    private Set<UUID> branchIds = new HashSet<>();

    public static AppUser create(String email, String passwordHash, String fullName) {
        AppUser u = new AppUser();
        u.setId(UUID.randomUUID());
        u.email = email;
        u.passwordHash = passwordHash;
        u.fullName = fullName;
        return u;
    }
}
