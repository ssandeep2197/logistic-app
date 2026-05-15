package com.helloworlds.tms.operations.domain;

import com.helloworlds.tms.platform.core.persistence.TenantScopedEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Power unit (the tractor).  Belongs to a Company.  Currently-assigned
 * drivers are a many-to-many via {@code truck_driver}; historical
 * assignments will live in a future {@code assignment_history} table.
 */
@Entity
@Table(name = "truck")
@Getter
@Setter
@NoArgsConstructor
public class Truck extends TenantScopedEntity {

    @Column(name = "company_id", nullable = false, columnDefinition = "uuid")
    private UUID companyId;

    /** {@code in_service}, {@code out_of_service}, {@code sold}, {@code totalled}. */
    @Column(nullable = false, length = 30)
    private String status = "in_service";

    @Column(length = 80)  private String nickname;        // internal name e.g. "Truck 12"
    @Column(length = 80)  private String make;
    @Column(length = 80)  private String model;
    private Integer year;
    @Column(length = 80)  private String engine;
    @Column(length = 17)  private String vin;
    @Column(name = "plate_number", length = 20) private String plateNumber;
    @Column(name = "plate_state",  length = 2)  private String plateState;
    @Column(name = "plate_expiry") private LocalDate plateExpiry;

    @Column(name = "insurance_policy_number", length = 80) private String insurancePolicyNumber;
    @Column(name = "insurance_carrier", length = 120) private String insuranceCarrier;
    @Column(name = "insurance_start") private LocalDate insuranceStart;
    @Column(name = "insurance_end")   private LocalDate insuranceEnd;

    /** FMCSA-required annual inspection (every 12 months). */
    @Column(name = "annual_inspection_expiry") private LocalDate annualInspectionExpiry;

    /** Free text for v1; {@code 'samsara'|'motive'|'geotab'|...}. */
    @Column(name = "eld_provider",  length = 40) private String eldProvider;
    @Column(name = "eld_device_id", length = 80) private String eldDeviceId;

    @Column(name = "in_service_date")     private LocalDate inServiceDate;
    @Column(name = "out_of_service_date") private LocalDate outOfServiceDate;

    @Column(columnDefinition = "text") private String notes;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "truck_driver",
        joinColumns = @JoinColumn(name = "truck_id"),
        inverseJoinColumns = @JoinColumn(name = "driver_id"))
    private Set<Driver> drivers = new HashSet<>();

    public static Truck create(UUID companyId, String vin) {
        Truck t = new Truck();
        t.setId(UUID.randomUUID());
        t.companyId = companyId;
        t.vin = vin;
        return t;
    }
}
