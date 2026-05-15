package com.helloworlds.tms.operations.web;

import com.helloworlds.tms.operations.domain.Company;
import com.helloworlds.tms.operations.domain.CompanyRepository;
import com.helloworlds.tms.platform.core.error.ConflictException;
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

import java.util.List;
import java.util.UUID;

/**
 * CRUD on the carrier's operating Companies (MC/DOT entities).
 * Bank details and document attachments hang off a separate endpoint
 * tree (added in phase 2.2 / 2.3) — they are NOT modeled here so
 * that the {@code customer:*} permissions remain orthogonal to the
 * accounting-only {@code accounting:bank:*} family.
 */
@RestController
@RequestMapping("/companies")
@RequiredArgsConstructor
public class CompanyController {

    private final CompanyRepository companies;
    private final PermissionEvaluator perms;

    @GetMapping
    @Transactional(readOnly = true)
    public Page<CompanyView> list(@RequestParam(required = false) String q,
                                   @RequestParam(defaultValue = "0") int page,
                                   @RequestParam(defaultValue = "50") int size) {
        perms.requireAll("company", "read");
        return companies.search(q, PageRequest.of(page, Math.min(size, 200))).map(CompanyView::from);
    }

    @GetMapping("/active")
    @Transactional(readOnly = true)
    public List<CompanyView> activeForDropdown() {
        return companies.findAllActive().stream().map(CompanyView::from).toList();
    }

    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    public CompanyView get(@PathVariable UUID id) {
        perms.requireAll("company", "read");
        return CompanyView.from(companies.findById(id).orElseThrow(() -> new NotFoundException("company", id)));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public CompanyView create(@Valid @RequestBody CompanyRequest req) {
        perms.requireAll("company", "create");
        companies.findByName(req.name()).ifPresent(c -> {
            throw new ConflictException("company name already exists: " + req.name());
        });
        Company c = Company.create(req.name());
        applyEdits(c, req);
        return CompanyView.from(companies.save(c));
    }

    @PutMapping("/{id}")
    @Transactional
    public CompanyView update(@PathVariable UUID id, @Valid @RequestBody CompanyRequest req) {
        perms.requireAll("company", "update");
        Company c = companies.findById(id).orElseThrow(() -> new NotFoundException("company", id));
        applyEdits(c, req);
        return CompanyView.from(c);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Transactional
    public void archive(@PathVariable UUID id) {
        perms.requireAll("company", "delete");
        Company c = companies.findById(id).orElseThrow(() -> new NotFoundException("company", id));
        // Soft delete: Trucks / Trailers / Drivers / Loads have FKs into company
        // with ON DELETE RESTRICT, so a hard delete would refuse if anything's
        // attached.  Setting status='inactive' is the right operational move.
        c.setStatus("inactive");
    }

    private void applyEdits(Company c, CompanyRequest r) {
        c.setName(r.name());
        c.setDba(r.dba());
        c.setMcNumber(blankToNull(r.mcNumber()));
        c.setDotNumber(blankToNull(r.dotNumber()));
        c.setTin(blankToNull(r.tin()));
        c.setAddressLine1(r.addressLine1());
        c.setAddressLine2(r.addressLine2());
        c.setCity(r.city());
        c.setStateCode(blankToNull(r.stateCode()));
        c.setPostalCode(r.postalCode());
        if (r.countryCode() != null) c.setCountryCode(r.countryCode());
        c.setPhone(r.phone());
        c.setEmail(r.email());
        c.setNotes(r.notes());
        if (r.status() != null) c.setStatus(r.status());
    }

    private static String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s;
    }

    public record CompanyRequest(
            @NotBlank @Size(max = 200) String name,
            @Size(max = 200) String dba,
            @Size(max = 20)  String mcNumber,
            @Size(max = 20)  String dotNumber,
            @Size(max = 20)  String tin,
            @Size(max = 200) String addressLine1,
            @Size(max = 200) String addressLine2,
            @Size(max = 120) String city,
            @Size(min = 0, max = 2) String stateCode,
            @Size(max = 20)  String postalCode,
            @Size(min = 2, max = 2) String countryCode,
            @Size(max = 40)  String phone,
            @Email @Size(max = 320) String email,
            @Pattern(regexp = "active|inactive|revoked") String status,
            @Size(max = 4000) String notes) {}

    public record CompanyView(
            UUID id, String name, String dba, String mcNumber, String dotNumber, String tin,
            String addressLine1, String addressLine2, String city, String stateCode,
            String postalCode, String countryCode, String phone, String email,
            String status, String notes) {
        static CompanyView from(Company c) {
            return new CompanyView(
                    c.getId(), c.getName(), c.getDba(), c.getMcNumber(), c.getDotNumber(), c.getTin(),
                    c.getAddressLine1(), c.getAddressLine2(), c.getCity(), c.getStateCode(),
                    c.getPostalCode(), c.getCountryCode(), c.getPhone(), c.getEmail(),
                    c.getStatus(), c.getNotes());
        }
    }
}
