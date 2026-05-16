package com.helloworlds.tms.operations.accounting;

import com.helloworlds.tms.platform.core.persistence.TenantScopedEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/**
 * Bank account belonging to either a {@code Company} (operating account)
 * or a {@code Driver} (payroll deposit).  Discriminated by
 * {@link #entityType} + {@link #entityId} rather than two FK columns so a
 * single audit / reveal codepath covers both.
 * <p>
 * <b>Sensitive columns are intentionally not mapped here.</b> The DB has
 * {@code account_number_encrypted} and {@code routing_number_encrypted}
 * BYTEA columns populated through native queries in {@link BankAccountService}
 * — keeping the cleartext out of any Java field means it can't leak through
 * {@code toString()}, a Jackson serialiser, or an accidental log line.
 * Only the last 4 digits live on the entity, for masked UI display.
 */
@Entity
@Table(name = "bank_account")
@Getter
@Setter
@NoArgsConstructor
public class BankAccount extends TenantScopedEntity {

    /** {@code company} or {@code driver}. */
    @Column(name = "entity_type", nullable = false, length = 20)
    private String entityType;

    @Column(name = "entity_id", nullable = false, columnDefinition = "uuid")
    private UUID entityId;

    @Column(length = 100) private String nickname;
    @Column(name = "bank_name", length = 200) private String bankName;
    @Column(name = "account_holder", length = 200) private String accountHolder;
    /** {@code checking} or {@code savings}. */
    @Column(name = "account_type", length = 20) private String accountType;

    /** Display-safe last 4 of the account number; cleartext, indexable. */
    @Column(name = "account_last4", length = 4) private String accountLast4;
    /** Display-safe last 4 of the routing number (string so leading zeros survive). */
    @Column(name = "routing_last4", length = 4) private String routingLast4;

    @Column(name = "swift_code", length = 20) private String swiftCode;

    /** FK into the documents table (Phase 2.3); image/PDF of the void check. */
    @Column(name = "void_check_document_id", columnDefinition = "uuid")
    private UUID voidCheckDocumentId;

    /** {@code active} or {@code archived}.  Soft-delete via archive. */
    @Column(nullable = false, length = 20)
    private String status = "active";

    @Column(name = "created_by", columnDefinition = "uuid") private UUID createdBy;
    @Column(name = "updated_by", columnDefinition = "uuid") private UUID updatedBy;
}
