package com.helloworlds.tms.operations.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface DriverRepository extends JpaRepository<Driver, UUID> {

    @Query("""
        SELECT d FROM Driver d
        WHERE (:companyId IS NULL OR d.companyId = :companyId)
          AND (:status    IS NULL OR d.status    = :status)
          AND (:q         IS NULL OR LOWER(d.firstName)     LIKE LOWER(CONCAT('%', :q, '%'))
                                  OR LOWER(d.lastName)      LIKE LOWER(CONCAT('%', :q, '%'))
                                  OR LOWER(COALESCE(d.licenseNumber,'')) LIKE LOWER(CONCAT('%', :q, '%')))
        ORDER BY d.lastName, d.firstName
        """)
    Page<Driver> search(@Param("companyId") UUID companyId,
                         @Param("status") String status,
                         @Param("q") String q,
                         Pageable pageable);

    /** Drivers whose license expires within {@code days} days — for compliance alerts. */
    @Query("""
        SELECT d FROM Driver d
        WHERE d.status = 'active'
          AND d.licenseExpiry IS NOT NULL
          AND d.licenseExpiry <= :cutoff
        ORDER BY d.licenseExpiry
        """)
    List<Driver> licenseExpiringBy(@Param("cutoff") LocalDate cutoff);
}
