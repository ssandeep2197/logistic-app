package com.helloworlds.tms.operations.domain;

import com.helloworlds.tms.platform.core.persistence.TenantScopedEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * One freight job — pickup somewhere, deliver somewhere, customer pays a
 * rate, driver moves it.  Single pickup / single delivery in v1; multi-stop
 * loads get a separate {@code stop} table when a customer requires it.
 * <p>
 * Rate is stored in cents ({@code BIGINT}) to avoid floating-point drift.
 * The web layer accepts / returns {@code BigDecimal} for ergonomics.
 */
@Entity
@Table(name = "load")
@Getter
@Setter
@NoArgsConstructor
public class Load extends TenantScopedEntity {

    /** Carrier-visible identifier — e.g. {@code "L-2026-001"} — unique per tenant. */
    @Column(name = "reference_number", nullable = false, length = 40)
    private String referenceNumber;

    @Column(name = "customer_id", nullable = false, columnDefinition = "uuid")
    private UUID customerId;

    /** {@link Status} values stored as lowercase strings. */
    @Column(nullable = false, length = 20)
    private String status = Status.PLANNED.code;

    /** Nullable until the rate is confirmed (e.g. by the LLM rate-con parser). */
    @Column(name = "rate_cents")
    private Long rateCents;

    @Column(name = "rate_currency", nullable = false, length = 3)
    private String rateCurrency = "USD";

    @Column(name = "pickup_location", nullable = false, columnDefinition = "text")
    private String pickupLocation;

    @Column(name = "pickup_window_start")
    private Instant pickupWindowStart;

    @Column(name = "pickup_window_end")
    private Instant pickupWindowEnd;

    @Column(name = "delivery_location", nullable = false, columnDefinition = "text")
    private String deliveryLocation;

    @Column(name = "delivery_window_start")
    private Instant deliveryWindowStart;

    @Column(name = "delivery_window_end")
    private Instant deliveryWindowEnd;

    /** Free text v1; replace with a Driver FK when the driver entity ships. */
    @Column(name = "assigned_driver_name", length = 200)
    private String assignedDriverName;

    @Column(columnDefinition = "text")
    private String notes;

    @Column(name = "delivered_at")
    private Instant deliveredAt;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    public static Load create(UUID customerId, String referenceNumber,
                              String pickupLocation, String deliveryLocation) {
        Load l = new Load();
        l.setId(UUID.randomUUID());
        l.customerId = customerId;
        l.referenceNumber = referenceNumber;
        l.pickupLocation = pickupLocation;
        l.deliveryLocation = deliveryLocation;
        return l;
    }

    public enum Status {
        PLANNED("planned"),
        IN_TRANSIT("in_transit"),
        DELIVERED("delivered"),
        CANCELLED("cancelled");

        public final String code;
        Status(String code) { this.code = code; }

        public static Status of(String code) {
            for (Status s : values()) if (s.code.equals(code)) return s;
            throw new IllegalArgumentException("unknown status: " + code);
        }
    }
}
