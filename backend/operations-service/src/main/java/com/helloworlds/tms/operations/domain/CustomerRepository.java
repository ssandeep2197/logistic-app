package com.helloworlds.tms.operations.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface CustomerRepository extends JpaRepository<Customer, UUID> {

    Optional<Customer> findByName(String name);

    /** Used by the load form's customer dropdown — active only. */
    @Query("SELECT c FROM Customer c WHERE c.status = 'active' ORDER BY c.name")
    java.util.List<Customer> findAllActive();

    @Query("""
        SELECT c FROM Customer c
        WHERE (:q IS NULL OR LOWER(c.name) LIKE LOWER(CONCAT('%', :q, '%')))
        """)
    Page<Customer> search(@Param("q") String q, Pageable pageable);
}
