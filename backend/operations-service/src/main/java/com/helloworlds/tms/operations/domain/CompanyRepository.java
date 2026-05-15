package com.helloworlds.tms.operations.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CompanyRepository extends JpaRepository<Company, UUID> {

    Optional<Company> findByName(String name);

    @Query("SELECT c FROM Company c WHERE c.status = 'active' ORDER BY c.name")
    List<Company> findAllActive();

    @Query("""
        SELECT c FROM Company c
        WHERE (:q IS NULL OR LOWER(c.name) LIKE LOWER(CONCAT('%', :q, '%'))
                          OR c.mcNumber = :q OR c.dotNumber = :q)
        """)
    Page<Company> search(@Param("q") String q, Pageable pageable);
}
