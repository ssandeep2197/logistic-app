package com.helloworlds.tms.operations.domain;

import com.helloworlds.tms.platform.core.persistence.TenantScopedEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Freight container — dry van, reefer, flatbed, etc.  Belongs to a Company.
 * Trailers are not assigned to drivers; they're attached to whichever truck
 * is hauling them for the current load.
 */
@Entity
@Table(name = "trailer")
@Getter
@Setter
@NoArgsConstructor
public class Trailer extends TenantScopedEntity {

    @Column(name = "company_id", nullable = false, columnDefinition = "uuid")
    private UUID companyId;

    /** {@code in_service}, {@code out_of_service}, {@code sold}, {@code totalled}. */
    @Column(nullable = false, length = 30)
    private String status = "in_service";

    @Column(length = 80) private String nickname;

    /** {@code dry_van}, {@code reefer}, {@code flatbed}, {@code step_deck}, {@code tanker}, {@code container}, {@code other}. */
    @Column(name = "trailer_type", length = 30) private String trailerType;
    /** {@code swing}, {@code roll_up}, {@code curtain}, etc. */
    @Column(name = "door_type",    length = 30) private String doorType;

    @Column(length = 80)  private String make;
    @Column(length = 80)  private String model;
    private Integer year;
    @Column(length = 17)  private String vin;
    @Column(name = "plate_number", length = 20) private String plateNumber;
    @Column(name = "plate_state",  length = 2)  private String plateState;
    @Column(name = "plate_expiry") private LocalDate plateExpiry;

    @Column(name = "annual_inspection_expiry") private LocalDate annualInspectionExpiry;

    @Column(name = "tracking_provider",  length = 40) private String trackingProvider;
    @Column(name = "tracking_device_id", length = 80) private String trackingDeviceId;

    @Column(name = "in_service_date")     private LocalDate inServiceDate;
    @Column(name = "out_of_service_date") private LocalDate outOfServiceDate;

    @Column(columnDefinition = "text") private String notes;

    public static Trailer create(UUID companyId, String vin) {
        Trailer t = new Trailer();
        t.setId(UUID.randomUUID());
        t.companyId = companyId;
        t.vin = vin;
        return t;
    }
}
