package com.helloworlds.tms.operations.domain;

import com.helloworlds.tms.platform.core.persistence.TenantScopedEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/**
 * A legal entity owned by a Tenant that holds MC/DOT numbers and operates
 * loads.  One Tenant → many Companies (multi-MC structures are common —
 * parent + subsidiaries each with their own authority).
 * <p>
 * Bank details, file attachments (W9, COI, MC certificate, void check),
 * and compliance event history are kept in separate tables added by later
 * migrations.  This entity is the relational center those tables FK into.
 */
@Entity
@Table(name = "company")
@Getter
@Setter
@NoArgsConstructor
public class Company extends TenantScopedEntity {

    @Column(nullable = false, length = 200)
    private String name;

    /** "Doing Business As" — optional trade name. */
    @Column(length = 200)
    private String dba;

    /**
     * FMCSA Motor Carrier number — globally unique across all tenants by
     * regulator.  Nullable until issued, but two companies can never share one.
     */
    @Column(name = "mc_number", length = 20)
    private String mcNumber;

    /** USDOT number — same uniqueness story as MC. */
    @Column(name = "dot_number", length = 20)
    private String dotNumber;

    /** Federal EIN (XX-XXXXXXX) or SSN.  Sensitive — log only the last 4 if you log at all. */
    @Column(length = 20)
    private String tin;

    @Column(name = "address_line1", length = 200) private String addressLine1;
    @Column(name = "address_line2", length = 200) private String addressLine2;
    @Column(length = 120) private String city;
    @Column(name = "state_code", length = 2) private String stateCode;
    @Column(name = "postal_code", length = 20) private String postalCode;
    @Column(name = "country_code", nullable = false, length = 2) private String countryCode = "US";
    @Column(length = 40)  private String phone;
    @Column(length = 320) private String email;

    /** {@code active}, {@code inactive}, {@code revoked} (MC/DOT pulled). */
    @Column(nullable = false, length = 20)
    private String status = "active";

    @Column(columnDefinition = "text")
    private String notes;

    public static Company create(String name) {
        Company c = new Company();
        c.setId(UUID.randomUUID());
        c.name = name;
        return c;
    }
}
