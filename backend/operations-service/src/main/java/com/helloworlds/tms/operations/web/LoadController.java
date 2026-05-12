package com.helloworlds.tms.operations.web;

import com.helloworlds.tms.operations.domain.Customer;
import com.helloworlds.tms.operations.domain.CustomerRepository;
import com.helloworlds.tms.operations.domain.Load;
import com.helloworlds.tms.operations.domain.LoadRepository;
import com.helloworlds.tms.platform.core.error.ConflictException;
import com.helloworlds.tms.platform.core.error.DomainException;
import com.helloworlds.tms.platform.core.error.NotFoundException;
import com.helloworlds.tms.platform.security.rbac.PermissionEvaluator;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Load CRUD + status transitions.  Status is a small state machine; the
 * server enforces legal transitions so the frontend can't drop a load
 * straight from planned → invoiced (something we don't even support yet).
 */
@RestController
@RequestMapping("/loads")
@RequiredArgsConstructor
public class LoadController {

    private final LoadRepository loads;
    private final CustomerRepository customers;
    private final PermissionEvaluator perms;

    /**
     * Legal status transitions.  Any transition not listed → 409.
     * planned    → in_transit | cancelled
     * in_transit → delivered  | cancelled
     * delivered  → (terminal)
     * cancelled  → (terminal)
     */
    private static final Map<Load.Status, Set<Load.Status>> ALLOWED = Map.of(
            Load.Status.PLANNED,    Set.of(Load.Status.IN_TRANSIT, Load.Status.CANCELLED),
            Load.Status.IN_TRANSIT, Set.of(Load.Status.DELIVERED,  Load.Status.CANCELLED),
            Load.Status.DELIVERED,  Set.of(),
            Load.Status.CANCELLED,  Set.of());

    @GetMapping
    @Transactional(readOnly = true)
    public Page<LoadView> list(@RequestParam(required = false) String status,
                                @RequestParam(required = false) UUID customerId,
                                @RequestParam(required = false) String q,
                                @RequestParam(defaultValue = "0") int page,
                                @RequestParam(defaultValue = "50") int size) {
        perms.requireAll("load", "read");
        return loads.search(status, customerId, q, PageRequest.of(page, Math.min(size, 200)))
                    .map(LoadView::from);
    }

    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    public LoadView get(@PathVariable UUID id) {
        perms.requireAll("load", "read");
        return LoadView.from(loads.findById(id).orElseThrow(() -> new NotFoundException("load", id)));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public LoadView create(@Valid @RequestBody LoadRequest req) {
        perms.requireAll("load", "create");

        if (loads.existsByReferenceNumber(req.referenceNumber())) {
            throw new ConflictException("reference number already exists: " + req.referenceNumber());
        }
        // Validate customer exists in THIS tenant — RLS makes the cross-tenant
        // case impossible to read, but a typo'd UUID still needs a clear error.
        Customer customer = customers.findById(req.customerId())
                .orElseThrow(() -> new NotFoundException("customer", req.customerId()));
        if (!"active".equals(customer.getStatus())) {
            throw new DomainException("customer_archived", 409, "customer is archived");
        }

        Load load = Load.create(customer.getId(), req.referenceNumber(),
                                 req.pickupLocation(), req.deliveryLocation());
        applyEdits(load, req);
        return LoadView.from(loads.save(load));
    }

    @PutMapping("/{id}")
    @Transactional
    public LoadView update(@PathVariable UUID id, @Valid @RequestBody LoadRequest req) {
        perms.requireAll("load", "update");
        Load load = loads.findById(id).orElseThrow(() -> new NotFoundException("load", id));
        // Customer can be swapped via PUT, but not arbitrary fields like status —
        // status moves only through /loads/{id}/status to keep the state machine honest.
        if (!load.getCustomerId().equals(req.customerId())) {
            customers.findById(req.customerId())
                    .orElseThrow(() -> new NotFoundException("customer", req.customerId()));
            load.setCustomerId(req.customerId());
        }
        load.setReferenceNumber(req.referenceNumber());
        applyEdits(load, req);
        return LoadView.from(load);
    }

    @PostMapping("/{id}/status")
    @Transactional
    public LoadView changeStatus(@PathVariable UUID id, @Valid @RequestBody StatusChange req) {
        perms.requireAll("load", "update");
        Load load = loads.findById(id).orElseThrow(() -> new NotFoundException("load", id));

        Load.Status from = Load.Status.of(load.getStatus());
        Load.Status to;
        try { to = Load.Status.of(req.status()); }
        catch (IllegalArgumentException e) {
            throw new DomainException("invalid_status", 400, e.getMessage());
        }

        Set<Load.Status> legal = ALLOWED.getOrDefault(from, Set.of());
        if (!legal.contains(to)) {
            throw new DomainException("illegal_transition", 409,
                    "cannot transition from " + from.code + " to " + to.code);
        }

        load.setStatus(to.code);
        if (to == Load.Status.DELIVERED) load.setDeliveredAt(Instant.now());
        if (to == Load.Status.CANCELLED) load.setCancelledAt(Instant.now());
        return LoadView.from(load);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Transactional
    public void delete(@PathVariable UUID id) {
        perms.requireAll("load", "update");
        Load load = loads.findById(id).orElseThrow(() -> new NotFoundException("load", id));
        // Hard-delete is fine for now; once invoicing references loads, switch
        // to soft-delete via a "deleted" status.
        loads.delete(load);
    }

    private void applyEdits(Load l, LoadRequest req) {
        l.setPickupLocation(req.pickupLocation());
        l.setPickupWindowStart(req.pickupWindowStart());
        l.setPickupWindowEnd(req.pickupWindowEnd());
        l.setDeliveryLocation(req.deliveryLocation());
        l.setDeliveryWindowStart(req.deliveryWindowStart());
        l.setDeliveryWindowEnd(req.deliveryWindowEnd());
        l.setAssignedDriverName(req.assignedDriverName());
        l.setNotes(req.notes());
        if (req.rate() != null) {
            // BigDecimal → cents.  Reject anything more granular than $0.01.
            l.setRateCents(req.rate().setScale(2, RoundingMode.UNNECESSARY)
                                       .movePointRight(2).longValueExact());
            l.setRateCurrency(req.rateCurrency() == null ? "USD" : req.rateCurrency());
        } else {
            l.setRateCents(null);
        }
    }

    // ── DTOs ──────────────────────────────────────────────────────────────

    public record LoadRequest(
            @NotBlank @Size(max = 40) @Pattern(regexp = "[\\w\\-./]+", message = "letters, numbers, -._/ only")
            String referenceNumber,
            @NotNull UUID customerId,
            @NotBlank @Size(max = 1000) String pickupLocation,
            Instant pickupWindowStart,
            Instant pickupWindowEnd,
            @NotBlank @Size(max = 1000) String deliveryLocation,
            Instant deliveryWindowStart,
            Instant deliveryWindowEnd,
            @Size(max = 200) String assignedDriverName,
            @Size(max = 4000) String notes,
            @DecimalMin(value = "0.00", inclusive = true) BigDecimal rate,
            @Size(min = 3, max = 3) String rateCurrency) {}

    public record StatusChange(@NotBlank String status) {}

    public record LoadView(
            UUID id, String referenceNumber, UUID customerId,
            String status, BigDecimal rate, String rateCurrency,
            String pickupLocation, Instant pickupWindowStart, Instant pickupWindowEnd,
            String deliveryLocation, Instant deliveryWindowStart, Instant deliveryWindowEnd,
            String assignedDriverName, String notes,
            Instant deliveredAt, Instant cancelledAt,
            Instant createdAt, Instant updatedAt) {
        static LoadView from(Load l) {
            BigDecimal rate = l.getRateCents() == null
                    ? null
                    : BigDecimal.valueOf(l.getRateCents()).movePointLeft(2);
            return new LoadView(
                    l.getId(), l.getReferenceNumber(), l.getCustomerId(),
                    l.getStatus(), rate, l.getRateCurrency(),
                    l.getPickupLocation(), l.getPickupWindowStart(), l.getPickupWindowEnd(),
                    l.getDeliveryLocation(), l.getDeliveryWindowStart(), l.getDeliveryWindowEnd(),
                    l.getAssignedDriverName(), l.getNotes(),
                    l.getDeliveredAt(), l.getCancelledAt(),
                    l.getCreatedAt(), l.getUpdatedAt());
        }
    }
}
