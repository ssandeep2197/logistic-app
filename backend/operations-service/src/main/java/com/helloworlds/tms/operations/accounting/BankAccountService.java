package com.helloworlds.tms.operations.accounting;

import com.helloworlds.tms.operations.domain.CompanyRepository;
import com.helloworlds.tms.operations.domain.DriverRepository;
import com.helloworlds.tms.platform.core.error.NotFoundException;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * The seam between callers and the pgcrypto-protected columns on
 * {@link BankAccount}.  Cleartext account numbers and routing numbers
 * never leave this class except through {@link #reveal} (which is gated
 * by the {@code bank_account:reveal:all} permission upstream and audited
 * on every call).
 * <p>
 * The strategy:
 * <ul>
 *   <li><b>Create</b> — one native INSERT that wraps the secrets in
 *       {@code pgp_sym_encrypt(:plaintext, :key, 'cipher-algo=aes256')}
 *       so the row is born encrypted; no two-step flow that could leave a
 *       half-populated row if the transaction rolls back between steps.</li>
 *   <li><b>Update</b> — non-sensitive fields go through the JPA entity; if
 *       the request includes a new account/routing number, a follow-up
 *       native UPDATE re-encrypts just those bytes.</li>
 *   <li><b>Reveal</b> — read-only native query, audited.</li>
 *   <li><b>Archive</b> — soft delete; flips status, never drops the row,
 *       so an audit trail outlives the account.</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class BankAccountService {

    private final EntityManager em;
    private final BankAccountRepository accounts;
    private final BankAccountAuditRepository audits;
    private final CompanyRepository companies;
    private final DriverRepository drivers;
    private final BankAccountEncryptionKey key;

    @Transactional
    public BankAccount create(CreateRequest req, UUID actorUserId, String actorIp) {
        requireOwnerExists(req.entityType(), req.entityId());

        UUID id = UUID.randomUUID();
        em.createNativeQuery("""
                INSERT INTO operations.bank_account
                    (id, tenant_id, version, created_at, updated_at,
                     entity_type, entity_id, nickname, bank_name, account_holder,
                     account_type,
                     account_number_encrypted, account_last4,
                     routing_number_encrypted, routing_last4,
                     swift_code, void_check_document_id,
                     status, created_by, updated_by)
                VALUES
                    (:id, current_setting('app.current_tenant')::uuid, 0, NOW(), NOW(),
                     :entityType, :entityId, :nickname, :bankName, :accountHolder,
                     :accountType,
                     pgp_sym_encrypt(:account, :key, 'cipher-algo=aes256'), :accountLast4,
                     pgp_sym_encrypt(:routing, :key, 'cipher-algo=aes256'), :routingLast4,
                     :swiftCode, :voidCheckDocumentId,
                     'active', :actor, :actor)
                """)
                .setParameter("id", id)
                .setParameter("entityType", req.entityType())
                .setParameter("entityId", req.entityId())
                .setParameter("nickname", req.nickname())
                .setParameter("bankName", req.bankName())
                .setParameter("accountHolder", req.accountHolder())
                .setParameter("accountType", req.accountType())
                .setParameter("account", req.accountNumber())
                .setParameter("accountLast4", last4(req.accountNumber()))
                .setParameter("routing", req.routingNumber())
                .setParameter("routingLast4", last4(req.routingNumber()))
                .setParameter("swiftCode", req.swiftCode())
                .setParameter("voidCheckDocumentId", req.voidCheckDocumentId())
                .setParameter("actor", actorUserId)
                .setParameter("key", key.key())
                .executeUpdate();

        // Drop any stale L1 view so the follow-up findById sees the row
        // we just inserted via native SQL.
        em.flush();
        em.clear();

        BankAccount ba = accounts.findById(id)
                .orElseThrow(() -> new IllegalStateException("insert succeeded but row not visible: " + id));
        audit(id, "create", actorUserId, actorIp);
        return ba;
    }

    @Transactional
    public BankAccount update(UUID id, UpdateRequest req, UUID actorUserId, String actorIp) {
        BankAccount ba = accounts.findById(id)
                .orElseThrow(() -> new NotFoundException("bank_account", id));

        if (req.nickname()       != null) ba.setNickname(req.nickname());
        if (req.bankName()       != null) ba.setBankName(req.bankName());
        if (req.accountHolder()  != null) ba.setAccountHolder(req.accountHolder());
        if (req.accountType()    != null) ba.setAccountType(req.accountType());
        if (req.swiftCode()      != null) ba.setSwiftCode(req.swiftCode());
        if (req.voidCheckDocumentId() != null) ba.setVoidCheckDocumentId(req.voidCheckDocumentId());
        ba.setUpdatedBy(actorUserId);

        if (req.accountNumber() != null && !req.accountNumber().isBlank()) {
            em.createNativeQuery("""
                    UPDATE operations.bank_account SET
                        account_number_encrypted = pgp_sym_encrypt(:account, :key, 'cipher-algo=aes256'),
                        account_last4 = :last4
                    WHERE id = :id
                    """)
                    .setParameter("account", req.accountNumber())
                    .setParameter("last4", last4(req.accountNumber()))
                    .setParameter("key", key.key())
                    .setParameter("id", id)
                    .executeUpdate();
            ba.setAccountLast4(last4(req.accountNumber()));
        }
        if (req.routingNumber() != null && !req.routingNumber().isBlank()) {
            em.createNativeQuery("""
                    UPDATE operations.bank_account SET
                        routing_number_encrypted = pgp_sym_encrypt(:routing, :key, 'cipher-algo=aes256'),
                        routing_last4 = :last4
                    WHERE id = :id
                    """)
                    .setParameter("routing", req.routingNumber())
                    .setParameter("last4", last4(req.routingNumber()))
                    .setParameter("key", key.key())
                    .setParameter("id", id)
                    .executeUpdate();
            ba.setRoutingLast4(last4(req.routingNumber()));
        }

        audit(id, "update", actorUserId, actorIp);
        return ba;
    }

    @Transactional(readOnly = true)
    public Revealed reveal(UUID id, UUID actorUserId, String actorIp) {
        // Touch the row through JPA first so we get a proper 404 if it's
        // missing (or filtered out by RLS) before we hand the decrypted
        // payload to anyone.
        accounts.findById(id).orElseThrow(() -> new NotFoundException("bank_account", id));

        Object[] row = (Object[]) em.createNativeQuery("""
                SELECT pgp_sym_decrypt(account_number_encrypted, :key),
                       pgp_sym_decrypt(routing_number_encrypted, :key)
                FROM operations.bank_account
                WHERE id = :id
                """)
                .setParameter("key", key.key())
                .setParameter("id", id)
                .getSingleResult();

        audit(id, "reveal", actorUserId, actorIp);
        return new Revealed((String) row[0], (String) row[1]);
    }

    @Transactional
    public void archive(UUID id, UUID actorUserId, String actorIp) {
        BankAccount ba = accounts.findById(id)
                .orElseThrow(() -> new NotFoundException("bank_account", id));
        if ("archived".equals(ba.getStatus())) return;
        ba.setStatus("archived");
        ba.setUpdatedBy(actorUserId);
        audit(id, "archive", actorUserId, actorIp);
    }

    private void requireOwnerExists(String entityType, UUID entityId) {
        boolean exists = switch (entityType) {
            case "company" -> companies.existsById(entityId);
            case "driver"  -> drivers.existsById(entityId);
            default -> throw new IllegalArgumentException("unknown entityType: " + entityType);
        };
        if (!exists) throw new NotFoundException(entityType, entityId);
    }

    private void audit(UUID bankAccountId, String action, UUID actorUserId, String actorIp) {
        BankAccountAudit a = new BankAccountAudit();
        a.setId(UUID.randomUUID());
        a.setBankAccountId(bankAccountId);
        a.setAction(action);
        a.setActorUserId(actorUserId);
        a.setActorIp(truncate(actorIp, 45));
        audits.save(a);
    }

    /** Last 4 digits (or fewer) of any input string; non-digits stripped. */
    static String last4(String s) {
        if (s == null) return null;
        String digits = s.replaceAll("\\D", "");
        return digits.length() <= 4 ? digits : digits.substring(digits.length() - 4);
    }

    private static String truncate(String s, int max) {
        return (s == null || s.length() <= max) ? s : s.substring(0, max);
    }

    public record CreateRequest(
            String entityType,
            UUID entityId,
            String nickname,
            String bankName,
            String accountHolder,
            String accountType,
            String accountNumber,
            String routingNumber,
            String swiftCode,
            UUID voidCheckDocumentId) {}

    public record UpdateRequest(
            String nickname,
            String bankName,
            String accountHolder,
            String accountType,
            /* Pass non-null only when rotating; null/blank means "keep existing". */
            String accountNumber,
            String routingNumber,
            String swiftCode,
            UUID voidCheckDocumentId) {}

    public record Revealed(String accountNumber, String routingNumber) {}
}
