package com.helloworlds.tms.operations.accounting.web;

import com.helloworlds.tms.operations.accounting.BankAccount;
import com.helloworlds.tms.operations.accounting.BankAccountRepository;
import com.helloworlds.tms.operations.accounting.BankAccountService;
import com.helloworlds.tms.platform.core.error.NotFoundException;
import com.helloworlds.tms.platform.security.rbac.PermissionEvaluator;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Bank account API.  Every method is gated by a {@code bank_account:*}
 * permission; the {@code Accounting} system role is the only role seeded
 * with these, so Tenant Admins cannot see or edit bank details.  Reading
 * the unmasked number requires the separate {@code bank_account:reveal:all}
 * permission and writes to {@code bank_account_audit}.
 */
@RestController
@RequestMapping("/bank-accounts")
@RequiredArgsConstructor
public class BankAccountController {

    private final BankAccountService service;
    private final BankAccountRepository accounts;
    private final PermissionEvaluator perms;

    /**
     * List the accounts attached to one company/driver.  Returns masked
     * views — last 4 digits only.  Includes archived rows by default so
     * the UI can show "history" tabs; pass {@code activeOnly=true} to
     * filter.
     */
    @GetMapping
    @Transactional(readOnly = true)
    public List<BankAccountView> list(
            @RequestParam @Pattern(regexp = "company|driver") String entityType,
            @RequestParam UUID entityId,
            @RequestParam(defaultValue = "false") boolean activeOnly) {
        perms.requireAll("bank_account", "read");
        List<BankAccount> rows = activeOnly
                ? accounts.findByEntityTypeAndEntityIdAndStatusOrderByCreatedAtDesc(entityType, entityId, "active")
                : accounts.findByEntityTypeAndEntityIdOrderByCreatedAtDesc(entityType, entityId);
        return rows.stream().map(BankAccountView::from).toList();
    }

    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    public BankAccountView get(@PathVariable UUID id) {
        perms.requireAll("bank_account", "read");
        return BankAccountView.from(
                accounts.findById(id).orElseThrow(() -> new NotFoundException("bank_account", id)));
    }

    /**
     * Unmasked account + routing numbers.  Requires {@code bank_account:reveal:all};
     * audited.  We intentionally don't return last4 here — UI should keep using
     * the row from {@code GET /{id}} and only swap the masked digits for the
     * revealed value in the field the user clicked.
     */
    @GetMapping("/{id}/reveal")
    @Transactional
    public RevealedView reveal(@PathVariable UUID id, HttpServletRequest http) {
        perms.requireAll("bank_account", "reveal");
        var r = service.reveal(id, perms.current().userId(), http.getRemoteAddr());
        return new RevealedView(id, r.accountNumber(), r.routingNumber());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BankAccountView create(@Valid @RequestBody CreateBody body, HttpServletRequest http) {
        perms.requireAll("bank_account", "create");
        var req = new BankAccountService.CreateRequest(
                body.entityType(), body.entityId(), body.nickname(), body.bankName(),
                body.accountHolder(), body.accountType(), body.accountNumber(),
                body.routingNumber(), body.swiftCode(), body.voidCheckDocumentId());
        return BankAccountView.from(service.create(req, perms.current().userId(), http.getRemoteAddr()));
    }

    @PutMapping("/{id}")
    public BankAccountView update(@PathVariable UUID id,
                                   @Valid @RequestBody UpdateBody body,
                                   HttpServletRequest http) {
        perms.requireAll("bank_account", "update");
        var req = new BankAccountService.UpdateRequest(
                body.nickname(), body.bankName(), body.accountHolder(), body.accountType(),
                body.accountNumber(), body.routingNumber(), body.swiftCode(), body.voidCheckDocumentId());
        return BankAccountView.from(service.update(id, req, perms.current().userId(), http.getRemoteAddr()));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void archive(@PathVariable UUID id, HttpServletRequest http) {
        perms.requireAll("bank_account", "delete");
        service.archive(id, perms.current().userId(), http.getRemoteAddr());
    }

    // ---------- DTOs ----------

    public record CreateBody(
            @NotBlank @Pattern(regexp = "company|driver") String entityType,
            @NotNull UUID entityId,
            @Size(max = 100) String nickname,
            @Size(max = 200) String bankName,
            @Size(max = 200) String accountHolder,
            @Pattern(regexp = "checking|savings") String accountType,
            @NotBlank @Size(min = 4, max = 34) String accountNumber,
            @NotBlank @Size(min = 4, max = 34) String routingNumber,
            @Size(max = 20) String swiftCode,
            UUID voidCheckDocumentId) {}

    public record UpdateBody(
            @Size(max = 100) String nickname,
            @Size(max = 200) String bankName,
            @Size(max = 200) String accountHolder,
            @Pattern(regexp = "checking|savings") String accountType,
            @Size(min = 4, max = 34) String accountNumber,
            @Size(min = 4, max = 34) String routingNumber,
            @Size(max = 20) String swiftCode,
            UUID voidCheckDocumentId) {}

    public record BankAccountView(
            UUID id, UUID tenantId, String entityType, UUID entityId,
            String nickname, String bankName, String accountHolder, String accountType,
            String accountLast4, String routingLast4, String swiftCode,
            UUID voidCheckDocumentId, String status,
            UUID createdBy, UUID updatedBy, Instant createdAt, Instant updatedAt) {
        static BankAccountView from(BankAccount b) {
            return new BankAccountView(
                    b.getId(), b.getTenantId(), b.getEntityType(), b.getEntityId(),
                    b.getNickname(), b.getBankName(), b.getAccountHolder(), b.getAccountType(),
                    b.getAccountLast4(), b.getRoutingLast4(), b.getSwiftCode(),
                    b.getVoidCheckDocumentId(), b.getStatus(),
                    b.getCreatedBy(), b.getUpdatedBy(), b.getCreatedAt(), b.getUpdatedAt());
        }
    }

    public record RevealedView(UUID id, String accountNumber, String routingNumber) {}
}
