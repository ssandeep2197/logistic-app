package com.helloworlds.tms.identity.domain;

import com.helloworlds.tms.platform.core.persistence.TenantScopedEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * Links an external identity provider's user (Google, GitHub, …) to a TMS
 * {@link AppUser}.  Provider's {@code subject} is the immutable id — never
 * email.  Email is captured at link time only for display in the user's
 * "connected accounts" screen.
 */
@Entity
@Table(name = "oauth_identity")
@Getter
@Setter
@NoArgsConstructor
public class OAuthIdentity extends TenantScopedEntity {

    @Column(nullable = false, length = 40)
    private String provider;

    @Column(nullable = false, length = 255)
    private String subject;

    @Column(name = "user_id", nullable = false, columnDefinition = "uuid")
    private UUID userId;

    @Column(name = "email_at_link", length = 320)
    private String emailAtLink;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "last_used_at")
    private Instant lastUsedAt;

    @PrePersist
    void onPersist() {
        if (createdAt == null) createdAt = Instant.now();
    }

    public static OAuthIdentity create(String provider, String subject, UUID userId, String email) {
        OAuthIdentity oi = new OAuthIdentity();
        oi.setId(UUID.randomUUID());
        oi.provider = provider;
        oi.subject = subject;
        oi.userId = userId;
        oi.emailAtLink = email;
        return oi;
    }
}
