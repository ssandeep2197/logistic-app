package com.helloworlds.tms.operations.web;

import com.helloworlds.tms.operations.domain.Company;
import com.helloworlds.tms.operations.domain.CompanyRepository;
import com.helloworlds.tms.operations.domain.Driver;
import com.helloworlds.tms.operations.domain.DriverRepository;
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

/**
 * CRUD on Drivers.  Bank details, drug test history, and full driver
 * qualification file checklist are handled by their own endpoint groups
 * (added in later phases) so an accidental grant of {@code driver:*}
 * permissions doesn't leak bank info.
 */
@RestController
@RequestMapping("/drivers")
@RequiredArgsConstructor
public class DriverController {

    private final DriverRepository drivers;
    private final CompanyRepository companies;
    private final PermissionEvaluator perms;

    @GetMapping
    @Transactional(readOnly = true)
    public Page<DriverView> list(@RequestParam(required = false) UUID companyId,
                                  @RequestParam(required = false) String status,
                                  @RequestParam(required = false) String q,
                                  @RequestParam(defaultValue = "0") int page,
                                  @RequestParam(defaultValue = "50") int size) {
        perms.requireAll("driver", "read");
        return drivers.search(companyId, status, q, PageRequest.of(page, Math.min(size, 200)))
                      .map(DriverView::from);
    }

    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    public DriverView get(@PathVariable UUID id) {
        perms.requireAll("driver", "read");
        return DriverView.from(drivers.findById(id).orElseThrow(() -> new NotFoundException("driver", id)));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public DriverView create(@Valid @RequestBody DriverRequest req) {
        perms.requireAll("driver", "create");
        Company company = companies.findById(req.companyId())
                .orElseThrow(() -> new NotFoundException("company", req.companyId()));
        Driver d = Driver.create(company.getId(), req.firstName(), req.lastName());
        applyEdits(d, req);
        return DriverView.from(drivers.save(d));
    }

    @PutMapping("/{id}")
    @Transactional
    public DriverView update(@PathVariable UUID id, @Valid @RequestBody DriverRequest req) {
        perms.requireAll("driver", "update");
        Driver d = drivers.findById(id).orElseThrow(() -> new NotFoundException("driver", id));
        if (!d.getCompanyId().equals(req.companyId())) {
            companies.findById(req.companyId())
                    .orElseThrow(() -> new NotFoundException("company", req.companyId()));
            d.setCompanyId(req.companyId());
        }
        applyEdits(d, req);
        return DriverView.from(d);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Transactional
    public void terminate(@PathVariable UUID id) {
        perms.requireAll("driver", "delete");
        Driver d = drivers.findById(id).orElseThrow(() -> new NotFoundException("driver", id));
        d.setStatus("terminated");
    }

    private void applyEdits(Driver d, DriverRequest r) {
        d.setFirstName(r.firstName());
        d.setLastName(r.lastName());
        if (r.status() != null) d.setStatus(r.status());
        d.setLicenseNumber(r.licenseNumber());
        d.setLicenseState(r.licenseState());
        d.setLicenseExpiry(r.licenseExpiry());
        d.setPhone(r.phone());
        d.setEmail(r.email());
        d.setAddressLine1(r.addressLine1());
        d.setAddressLine2(r.addressLine2());
        d.setCity(r.city());
        d.setStateCode(r.stateCode());
        d.setPostalCode(r.postalCode());
        if (r.countryCode() != null) d.setCountryCode(r.countryCode());
        d.setWorkAuthorization(r.workAuthorization());
        d.setWorkAuthExpiry(r.workAuthExpiry());
        d.setMedicalExamExpiry(r.medicalExamExpiry());
        d.setNotes(r.notes());
        // Note: pay_type / pay_rate and last_drug_test_* live here for v1 but
        // a future phase will move pay → payroll-service and drug tests → a
        // dedicated drug_test history table.  Mutation here for now.
        d.setPayType(r.payType());
        d.setPayRateCents(r.payRateCents());
    }

    public record DriverRequest(
            @NotNull UUID companyId,
            @NotBlank @Size(max = 100) String firstName,
            @NotBlank @Size(max = 100) String lastName,
            @Pattern(regexp = "active|terminated|on_leave|inactive_disqualified") String status,
            @Size(max = 40) String licenseNumber,
            @Size(min = 2, max = 2) String licenseState,
            LocalDate licenseExpiry,
            @Size(max = 40) String phone,
            @Email @Size(max = 320) String email,
            @Size(max = 200) String addressLine1,
            @Size(max = 200) String addressLine2,
            @Size(max = 120) String city,
            @Size(min = 0, max = 2) String stateCode,
            @Size(max = 20) String postalCode,
            @Size(min = 2, max = 2) String countryCode,
            @Pattern(regexp = "us_citizen|green_card|work_permit|non_us") String workAuthorization,
            LocalDate workAuthExpiry,
            LocalDate medicalExamExpiry,
            @Pattern(regexp = "per_mile|percent_of_load|hourly|flat|none") String payType,
            @PositiveOrZero Long payRateCents,
            @Size(max = 4000) String notes) {}

    public record DriverView(
            UUID id, UUID companyId, String status,
            String firstName, String lastName,
            String licenseNumber, String licenseState, LocalDate licenseExpiry,
            String phone, String email,
            String addressLine1, String addressLine2, String city, String stateCode,
            String postalCode, String countryCode,
            String workAuthorization, LocalDate workAuthExpiry,
            LocalDate medicalExamExpiry,
            LocalDate lastDrugTestDate, String lastDrugTestResult,
            String payType, Long payRateCents,
            String notes) {
        static DriverView from(Driver d) {
            return new DriverView(
                    d.getId(), d.getCompanyId(), d.getStatus(),
                    d.getFirstName(), d.getLastName(),
                    d.getLicenseNumber(), d.getLicenseState(), d.getLicenseExpiry(),
                    d.getPhone(), d.getEmail(),
                    d.getAddressLine1(), d.getAddressLine2(), d.getCity(), d.getStateCode(),
                    d.getPostalCode(), d.getCountryCode(),
                    d.getWorkAuthorization(), d.getWorkAuthExpiry(),
                    d.getMedicalExamExpiry(),
                    d.getLastDrugTestDate(), d.getLastDrugTestResult(),
                    d.getPayType(), d.getPayRateCents(),
                    d.getNotes());
        }
    }
}
