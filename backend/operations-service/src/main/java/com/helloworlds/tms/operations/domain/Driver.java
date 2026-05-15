package com.helloworlds.tms.operations.domain;

import com.helloworlds.tms.platform.core.persistence.TenantScopedEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.UUID;

/**
 * A person who drives a truck.  Belongs to a Company (whose MC/DOT they
 * operate under for HOS reporting).  Bank details, the full driver
 * qualification file, and drug-test history go in separate tables.
 */
@Entity
@Table(name = "driver")
@Getter
@Setter
@NoArgsConstructor
public class Driver extends TenantScopedEntity {

    @Column(name = "company_id", nullable = false, columnDefinition = "uuid")
    private UUID companyId;

    /** {@code active}, {@code terminated}, {@code on_leave}, {@code inactive_disqualified}. */
    @Column(nullable = false, length = 30)
    private String status = "active";

    @Column(name = "first_name", nullable = false, length = 100) private String firstName;
    @Column(name = "last_name",  nullable = false, length = 100) private String lastName;

    @Column(name = "license_number", length = 40) private String licenseNumber;
    @Column(name = "license_state",  length = 2)  private String licenseState;
    @Column(name = "license_expiry") private LocalDate licenseExpiry;

    @Column(length = 40)  private String phone;
    @Column(length = 320) private String email;

    @Column(name = "address_line1", length = 200) private String addressLine1;
    @Column(name = "address_line2", length = 200) private String addressLine2;
    @Column(length = 120) private String city;
    @Column(name = "state_code",   length = 2) private String stateCode;
    @Column(name = "postal_code",  length = 20) private String postalCode;
    @Column(name = "country_code", nullable = false, length = 2) private String countryCode = "US";

    /** {@code us_citizen}, {@code green_card}, {@code work_permit}, {@code non_us}. */
    @Column(name = "work_authorization", length = 30)
    private String workAuthorization;
    @Column(name = "work_auth_expiry") private LocalDate workAuthExpiry;

    @Column(name = "medical_exam_expiry") private LocalDate medicalExamExpiry;

    @Column(name = "last_drug_test_date")   private LocalDate lastDrugTestDate;
    @Column(name = "last_drug_test_result", length = 20)
    private String lastDrugTestResult;

    /** {@code per_mile}, {@code percent_of_load}, {@code hourly}, {@code flat}, {@code none}. */
    @Column(name = "pay_type", length = 20)
    private String payType;
    /** Cents per mile / per hour / flat amount, contextual to {@link #payType}. */
    @Column(name = "pay_rate_cents")
    private Long payRateCents;

    @Column(columnDefinition = "text") private String notes;

    public static Driver create(UUID companyId, String firstName, String lastName) {
        Driver d = new Driver();
        d.setId(UUID.randomUUID());
        d.companyId = companyId;
        d.firstName = firstName;
        d.lastName  = lastName;
        return d;
    }
}
