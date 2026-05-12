package com.helloworlds.tms.identity.settings;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * Global setting key/value, scoped to the whole SaaS — only the platform
 * owner can write these.  Value is a plain string; callers interpret it
 * (boolean as "true"/"false", numbers as decimals, etc.).
 * <p>
 * Keyed by the {@code key} column itself, not a synthetic UUID — there are
 * tens of these, never thousands, and string keys make queries readable.
 */
@Entity
@Table(name = "platform_setting")
@Getter
@Setter
@NoArgsConstructor
public class PlatformSetting {

    @Id
    @Column(name = "key", nullable = false, updatable = false, length = 80)
    private String key;

    @Column(name = "value", nullable = false, columnDefinition = "text")
    private String value;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /** UUID of the platform-admin user who last touched this row. */
    @Column(name = "updated_by", columnDefinition = "uuid")
    private UUID updatedBy;

    @PrePersist
    @PreUpdate
    void touch() { updatedAt = Instant.now(); }

    public static PlatformSetting of(String key, String value, UUID by) {
        PlatformSetting s = new PlatformSetting();
        s.key = key;
        s.value = value;
        s.updatedBy = by;
        return s;
    }
}
