package com.helloworlds.tms.operations.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface LoadRepository extends JpaRepository<Load, UUID> {

    Optional<Load> findByReferenceNumber(String referenceNumber);

    boolean existsByReferenceNumber(String referenceNumber);

    /**
     * Filtered + paged list for the load board.  Each filter is optional —
     * {@code NULL} means "any".  Searches reference number and notes.
     */
    @Query("""
        SELECT l FROM Load l
        WHERE (:status      IS NULL OR l.status = :status)
          AND (:customerId  IS NULL OR l.customerId = :customerId)
          AND (:q           IS NULL OR LOWER(l.referenceNumber) LIKE LOWER(CONCAT('%', :q, '%'))
                                    OR LOWER(l.pickupLocation)   LIKE LOWER(CONCAT('%', :q, '%'))
                                    OR LOWER(l.deliveryLocation) LIKE LOWER(CONCAT('%', :q, '%')))
        ORDER BY l.createdAt DESC
        """)
    Page<Load> search(@Param("status") String status,
                       @Param("customerId") UUID customerId,
                       @Param("q") String q,
                       Pageable pageable);
}
