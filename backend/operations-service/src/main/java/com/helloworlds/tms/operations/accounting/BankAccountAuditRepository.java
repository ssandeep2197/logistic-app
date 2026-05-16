package com.helloworlds.tms.operations.accounting;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface BankAccountAuditRepository extends JpaRepository<BankAccountAudit, UUID> {

    Page<BankAccountAudit> findByBankAccountIdOrderByAtDesc(UUID bankAccountId, Pageable pageable);
}
