package com.helloworlds.tms.operations.web;

import com.helloworlds.tms.operations.domain.Company;
import com.helloworlds.tms.operations.domain.CompanyRepository;
import com.helloworlds.tms.operations.domain.Trailer;
import com.helloworlds.tms.operations.domain.TrailerRepository;
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
import java.util.UUID;

@RestController
@RequestMapping("/trailers")
@RequiredArgsConstructor
public class TrailerController {

    private final TrailerRepository trailers;
    private final CompanyRepository companies;
    private final PermissionEvaluator perms;

    @GetMapping
    @Transactional(readOnly = true)
    public Page<TrailerView> list(@RequestParam(required = false) UUID companyId,
                                   @RequestParam(required = false) String status,
                                   @RequestParam(required = false) String q,
                                   @RequestParam(defaultValue = "0") int page,
                                   @RequestParam(defaultValue = "50") int size) {
        perms.requireAll("trailer", "read");
        return trailers.search(companyId, status, q, PageRequest.of(page, Math.min(size, 200)))
                       .map(TrailerView::from);
    }

    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    public TrailerView get(@PathVariable UUID id) {
        perms.requireAll("trailer", "read");
        return TrailerView.from(trailers.findById(id).orElseThrow(() -> new NotFoundException("trailer", id)));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public TrailerView create(@Valid @RequestBody TrailerRequest req) {
        perms.requireAll("trailer", "create");
        Company company = companies.findById(req.companyId())
                .orElseThrow(() -> new NotFoundException("company", req.companyId()));
        Trailer t = Trailer.create(company.getId(), req.vin());
        applyEdits(t, req);
        return TrailerView.from(trailers.save(t));
    }

    @PutMapping("/{id}")
    @Transactional
    public TrailerView update(@PathVariable UUID id, @Valid @RequestBody TrailerRequest req) {
        perms.requireAll("trailer", "update");
        Trailer t = trailers.findById(id).orElseThrow(() -> new NotFoundException("trailer", id));
        if (!t.getCompanyId().equals(req.companyId())) {
            companies.findById(req.companyId())
                    .orElseThrow(() -> new NotFoundException("company", req.companyId()));
            t.setCompanyId(req.companyId());
        }
        t.setVin(req.vin());
        applyEdits(t, req);
        return TrailerView.from(t);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Transactional
    public void retire(@PathVariable UUID id) {
        perms.requireAll("trailer", "delete");
        Trailer t = trailers.findById(id).orElseThrow(() -> new NotFoundException("trailer", id));
        t.setStatus("out_of_service");
        if (t.getOutOfServiceDate() == null) t.setOutOfServiceDate(LocalDate.now());
    }

    private void applyEdits(Trailer t, TrailerRequest r) {
        if (r.status() != null) t.setStatus(r.status());
        t.setNickname(r.nickname());
        t.setTrailerType(r.trailerType());
        t.setDoorType(r.doorType());
        t.setMake(r.make());
        t.setModel(r.model());
        t.setYear(r.year());
        t.setPlateNumber(r.plateNumber());
        t.setPlateState(r.plateState());
        t.setPlateExpiry(r.plateExpiry());
        t.setAnnualInspectionExpiry(r.annualInspectionExpiry());
        t.setTrackingProvider(r.trackingProvider());
        t.setTrackingDeviceId(r.trackingDeviceId());
        t.setInServiceDate(r.inServiceDate());
        t.setOutOfServiceDate(r.outOfServiceDate());
        t.setNotes(r.notes());
    }

    public record TrailerRequest(
            @NotNull UUID companyId,
            @Pattern(regexp = "in_service|out_of_service|sold|totalled") String status,
            @Size(max = 80) String nickname,
            @Pattern(regexp = "dry_van|reefer|flatbed|step_deck|tanker|container|other") String trailerType,
            @Size(max = 30) String doorType,
            @Size(max = 80) String make,
            @Size(max = 80) String model,
            @Min(1900) @Max(2100) Integer year,
            @NotBlank @Size(min = 11, max = 17) String vin,
            @Size(max = 20) String plateNumber,
            @Size(min = 2, max = 2) String plateState,
            LocalDate plateExpiry,
            LocalDate annualInspectionExpiry,
            @Size(max = 40) String trackingProvider,
            @Size(max = 80) String trackingDeviceId,
            LocalDate inServiceDate,
            LocalDate outOfServiceDate,
            @Size(max = 4000) String notes) {}

    public record TrailerView(
            UUID id, UUID companyId, String status, String nickname,
            String trailerType, String doorType, String make, String model, Integer year,
            String vin, String plateNumber, String plateState, LocalDate plateExpiry,
            LocalDate annualInspectionExpiry,
            String trackingProvider, String trackingDeviceId,
            LocalDate inServiceDate, LocalDate outOfServiceDate,
            String notes) {
        static TrailerView from(Trailer t) {
            return new TrailerView(
                    t.getId(), t.getCompanyId(), t.getStatus(), t.getNickname(),
                    t.getTrailerType(), t.getDoorType(), t.getMake(), t.getModel(), t.getYear(),
                    t.getVin(), t.getPlateNumber(), t.getPlateState(), t.getPlateExpiry(),
                    t.getAnnualInspectionExpiry(),
                    t.getTrackingProvider(), t.getTrackingDeviceId(),
                    t.getInServiceDate(), t.getOutOfServiceDate(),
                    t.getNotes());
        }
    }
}
