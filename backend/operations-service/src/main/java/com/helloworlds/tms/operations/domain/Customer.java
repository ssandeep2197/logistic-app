package com.helloworlds.tms.operations.domain;

import com.helloworlds.tms.platform.core.persistence.TenantScopedEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A shipper / broker that pays the carrier for moving freight.  In a
 * carrier-owned TMS this is the carrier's customer; in a brokerage TMS the
 * model expands (shipper + receiver + carrier) — we'll add those when a
 * brokerage tenant signs up.
 *
 * Inherits id, version, tenant_id, created_at, updated_at from
 * {@link TenantScopedEntity}.  Unique on (tenant_id, name).
 */
@Entity
@Table(name = "customer")
@Getter
@Setter
@NoArgsConstructor
public class Customer extends TenantScopedEntity {

    @Column(nullable = false, length = 200)
    private String name;

    @Column(name = "contact_name", length = 200)
    private String contactName;

    @Column(name = "contact_email", length = 320)
    private String contactEmail;

    @Column(name = "contact_phone", length = 40)
    private String contactPhone;

    /** Single free-form address field for v1; structured columns when invoicing arrives. */
    @Column(name = "billing_address", columnDefinition = "text")
    private String billingAddress;

    @Column(columnDefinition = "text")
    private String notes;

    /** {@code active} | {@code archived}.  Soft-delete; archived customers don't appear in dropdowns. */
    @Column(nullable = false, length = 20)
    private String status = "active";

    public static Customer create(String name) {
        Customer c = new Customer();
        c.setId(java.util.UUID.randomUUID());
        c.name = name;
        return c;
    }
}
