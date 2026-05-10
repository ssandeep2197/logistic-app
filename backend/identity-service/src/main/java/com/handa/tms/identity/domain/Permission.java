package com.handa.tms.identity.domain;

import com.handa.tms.platform.core.persistence.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "permission")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Permission extends BaseEntity {

    @Column(nullable = false, length = 80)
    private String resource;

    @Column(nullable = false, length = 40)
    private String action;

    @Column(nullable = false, length = 40)
    private String scope;

    @Column(columnDefinition = "text")
    private String description;

    /** Returns the canonical {@code resource:action:scope} string used in JWT claims. */
    public String code() {
        return resource + ":" + action + ":" + scope;
    }
}
