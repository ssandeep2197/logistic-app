package com.helloworlds.tms.identity.domain;

import com.helloworlds.tms.platform.core.persistence.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * A customer of the SaaS — one trucking company, brokerage, or 3PL.
 * Everything else (users, loads, invoices) is scoped to a tenant.
 * <p>
 * The {@code slug} is what we put in the host or path so a tenant has a stable
 * URL ({@code acme.tms.app} or {@code app.tms/acme}); the {@code id} is the
 * actual foreign key.
 */
@Entity
@Table(name = "tenant")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Tenant extends BaseEntity {

    @Column(nullable = false, unique = true, length = 64)
    private String slug;

    @Column(nullable = false, length = 200)
    private String name;

    /** {@code starter}, {@code growth}, {@code pro}, {@code enterprise}.  Plan limits decided later. */
    @Column(nullable = false, length = 40)
    private String plan = "starter";

    /** {@code active}, {@code suspended}, {@code closed}. */
    @Column(nullable = false, length = 20)
    private String status = "active";

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onPersist() {
        Instant now = Instant.now();
        if (createdAt == null) createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public static Tenant create(String slug, String name) {
        Tenant t = new Tenant();
        t.setId(UUID.randomUUID());
        t.slug = slug;
        t.name = name;
        return t;
    }
}
