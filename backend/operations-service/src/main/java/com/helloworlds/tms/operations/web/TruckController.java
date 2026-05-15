package com.helloworlds.tms.operations.web;

import com.helloworlds.tms.operations.domain.*;
import com.helloworlds.tms.platform.core.error.NotFoundException;
import com.helloworlds.tms.platform.security.rbac.PermissionEvaluator;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/trucks")
@RequiredArgsConstructor
public class TruckController {

    private final TruckRepository trucks;
    private final CompanyRepository companies;
    private final DriverRepository drivers;
    private final PermissionEvaluator perms;

    @GetMapping
    @Transactional(readOnly = true)
    public Page<TruckView> list(@RequestParam(required = false) UUID companyId,
                                 @RequestParam(required = false) String status,
                                 @RequestParam(required = false) String q,
                                 @RequestParam(defaultValue = "0") int page,
                                 @RequestParam(defaultValue = "50") int size) {
        perms.requireAll("truck", "read");
        return trucks.search(companyId, status, q, PageRequest.of(page, Math.min(size, 200)))
                     .map(TruckView::from);
    }

    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    public TruckView get(@PathVariable UUID id) {
        perms.requireAll("truck", "read");
        return TruckView.from(trucks.findById(id).orElseThrow(() -> new NotFoundException("truck", id)));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public TruckView create(@Valid @RequestBody TruckRequest req) {
        perms.requireAll("truck", "create");
        Company company = companies.findById(req.companyId())
                .orElseThrow(() -> new NotFoundException("company", req.companyId()));
        Truck t = Truck.create(company.getId(), req.vin());
        applyEdits(t, req);
        attachDrivers(t, req.driverIds());
        return TruckView.from(trucks.save(t));
    }

    @PutMapping("/{id}")
    @Transactional
    public TruckView update(@PathVariable UUID id, @Valid @RequestBody TruckRequest req) {
        perms.requireAll("truck", "update");
        Truck t = trucks.findById(id).orElseThrow(() -> new NotFoundException("truck", id));
        if (!t.getCompanyId().equals(req.companyId())) {
            companies.findById(req.companyId())
                    .orElseThrow(() -> new NotFoundException("company", req.companyId()));
            t.setCompanyId(req.companyId());
        }
        t.setVin(req.vin());
        applyEdits(t, req);
        t.getDrivers().clear();
        attachDrivers(t, req.driverIds());
        return TruckView.from(t);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Transactional
    public void retire(@PathVariable UUID id) {
        perms.requireAll("truck", "delete");
        Truck t = trucks.findById(id).orElseThrow(() -> new NotFoundException("truck", id));
        t.setStatus("out_of_service");
        if (t.getOutOfServiceDate() == null) t.setOutOfServiceDate(LocalDate.now());
    }

    private void applyEdits(Truck t, TruckRequest r) {
        if (r.status() != null) t.setStatus(r.status());
        t.setNickname(r.nickname());
        t.setMake(r.make());
        t.setModel(r.model());
        t.setYear(r.year());
        t.setEngine(r.engine());
        t.setPlateNumber(r.plateNumber());
        t.setPlateState(r.plateState());
        t.setPlateExpiry(r.plateExpiry());
        t.setInsurancePolicyNumber(r.insurancePolicyNumber());
        t.setInsuranceCarrier(r.insuranceCarrier());
        t.setInsuranceStart(r.insuranceStart());
        t.setInsuranceEnd(r.insuranceEnd());
        t.setAnnualInspectionExpiry(r.annualInspectionExpiry());
        t.setEldProvider(r.eldProvider());
        t.setEldDeviceId(r.eldDeviceId());
        t.setInServiceDate(r.inServiceDate());
        t.setOutOfServiceDate(r.outOfServiceDate());
        t.setNotes(r.notes());
    }

    private void attachDrivers(Truck t, List<UUID> ids) {
        if (ids == null || ids.isEmpty()) return;
        for (UUID id : ids) {
            Driver d = drivers.findById(id).orElseThrow(() -> new NotFoundException("driver", id));
            t.getDrivers().add(d);
        }
    }

    public record TruckRequest(
            @NotNull UUID companyId,
            @Pattern(regexp = "in_service|out_of_service|sold|totalled") String status,
            @Size(max = 80) String nickname,
            @Size(max = 80) String make,
            @Size(max = 80) String model,
            @Min(1900) @Max(2100) Integer year,
            @Size(max = 80) String engine,
            @NotBlank @Size(min = 11, max = 17) String vin,
            @Size(max = 20) String plateNumber,
            @Size(min = 2, max = 2) String plateState,
            LocalDate plateExpiry,
            @Size(max = 80)  String insurancePolicyNumber,
            @Size(max = 120) String insuranceCarrier,
            LocalDate insuranceStart,
            LocalDate insuranceEnd,
            LocalDate annualInspectionExpiry,
            @Size(max = 40) String eldProvider,
            @Size(max = 80) String eldDeviceId,
            LocalDate inServiceDate,
            LocalDate outOfServiceDate,
            @Size(max = 4000) String notes,
            List<UUID> driverIds) {}

    public record TruckView(
            UUID id, UUID companyId, String status, String nickname,
            String make, String model, Integer year, String engine,
            String vin, String plateNumber, String plateState, LocalDate plateExpiry,
            String insurancePolicyNumber, String insuranceCarrier,
            LocalDate insuranceStart, LocalDate insuranceEnd, LocalDate annualInspectionExpiry,
            String eldProvider, String eldDeviceId,
            LocalDate inServiceDate, LocalDate outOfServiceDate,
            String notes, Set<UUID> driverIds) {
        static TruckView from(Truck t) {
            return new TruckView(
                    t.getId(), t.getCompanyId(), t.getStatus(), t.getNickname(),
                    t.getMake(), t.getModel(), t.getYear(), t.getEngine(),
                    t.getVin(), t.getPlateNumber(), t.getPlateState(), t.getPlateExpiry(),
                    t.getInsurancePolicyNumber(), t.getInsuranceCarrier(),
                    t.getInsuranceStart(), t.getInsuranceEnd(), t.getAnnualInspectionExpiry(),
                    t.getEldProvider(), t.getEldDeviceId(),
                    t.getInServiceDate(), t.getOutOfServiceDate(),
                    t.getNotes(),
                    t.getDrivers().stream().map(Driver::getId).collect(java.util.stream.Collectors.toSet()));
        }
    }
}
