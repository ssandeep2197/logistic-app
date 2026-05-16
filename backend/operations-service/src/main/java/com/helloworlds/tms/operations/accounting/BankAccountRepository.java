package com.helloworlds.tms.operations.accounting;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface BankAccountRepository extends JpaRepository<BankAccount, UUID> {

    /** All accounts (active + archived) attached to one entity, newest first. */
    List<BankAccount> findByEntityTypeAndEntityIdOrderByCreatedAtDesc(String entityType, UUID entityId);

    /** Just the active ones — what most UI list views want. */
    List<BankAccount> findByEntityTypeAndEntityIdAndStatusOrderByCreatedAtDesc(
            String entityType, UUID entityId, String status);
}
