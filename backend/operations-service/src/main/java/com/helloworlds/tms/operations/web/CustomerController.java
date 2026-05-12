package com.helloworlds.tms.operations.web;

import com.helloworlds.tms.operations.domain.Customer;
import com.helloworlds.tms.operations.domain.CustomerRepository;
import com.helloworlds.tms.platform.core.error.ConflictException;
import com.helloworlds.tms.platform.core.error.NotFoundException;
import com.helloworlds.tms.platform.security.rbac.PermissionEvaluator;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Customer CRUD.  Permission codes follow {@code customer:&lt;action&gt;:all}.
 * Soft-delete: DELETE flips status to 'archived' rather than removing the row,
 * because Loads have a foreign key to customer.  Hard delete requires removing
 * all loads first (we don't expose that yet).
 */
@RestController
@RequestMapping("/customers")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerRepository customers;
    private final PermissionEvaluator perms;

    @GetMapping
    @Transactional(readOnly = true)
    public Page<CustomerView> list(@RequestParam(required = false) String q,
                                    @RequestParam(defaultValue = "0") int page,
                                    @RequestParam(defaultValue = "50") int size) {
        perms.requireAll("customer", "read");
        Pageable pageable = PageRequest.of(page, Math.min(size, 200), Sort.by("name"));
        return customers.search(q, pageable).map(CustomerView::from);
    }

    @GetMapping("/active")
    @Transactional(readOnly = true)
    public List<CustomerView> activeForDropdown() {
        // No explicit gate — every authed tenant user can read the list of
        // their tenant's active customers; the load form needs it.
        return customers.findAllActive().stream().map(CustomerView::from).toList();
    }

    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    public CustomerView get(@PathVariable UUID id) {
        perms.requireAll("customer", "read");
        return CustomerView.from(customers.findById(id).orElseThrow(() -> new NotFoundException("customer", id)));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public CustomerView create(@Valid @RequestBody CustomerRequest req) {
        perms.requireAll("customer", "create");
        customers.findByName(req.name()).ifPresent(c -> {
            throw new ConflictException("customer name already exists: " + req.name());
        });
        Customer c = Customer.create(req.name());
        applyEdits(c, req);
        return CustomerView.from(customers.save(c));
    }

    @PutMapping("/{id}")
    @Transactional
    public CustomerView update(@PathVariable UUID id, @Valid @RequestBody CustomerRequest req) {
        perms.requireAll("customer", "update");
        Customer c = customers.findById(id).orElseThrow(() -> new NotFoundException("customer", id));
        applyEdits(c, req);
        return CustomerView.from(c);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Transactional
    public void archive(@PathVariable UUID id) {
        perms.requireAll("customer", "delete");
        Customer c = customers.findById(id).orElseThrow(() -> new NotFoundException("customer", id));
        c.setStatus("archived");
    }

    private void applyEdits(Customer c, CustomerRequest req) {
        c.setName(req.name());
        c.setContactName(req.contactName());
        c.setContactEmail(req.contactEmail());
        c.setContactPhone(req.contactPhone());
        c.setBillingAddress(req.billingAddress());
        c.setNotes(req.notes());
    }

    // ── DTOs ──────────────────────────────────────────────────────────────

    public record CustomerRequest(
            @NotBlank @Size(max = 200) String name,
            @Size(max = 200) String contactName,
            @Email @Size(max = 320) String contactEmail,
            @Size(max = 40) String contactPhone,
            @Size(max = 2000) String billingAddress,
            @Size(max = 4000) String notes) {}

    public record CustomerView(
            UUID id, String name, String contactName, String contactEmail,
            String contactPhone, String billingAddress, String notes, String status) {
        static CustomerView from(Customer c) {
            return new CustomerView(
                    c.getId(), c.getName(),
                    c.getContactName(), c.getContactEmail(), c.getContactPhone(),
                    c.getBillingAddress(), c.getNotes(), c.getStatus());
        }
    }
}
