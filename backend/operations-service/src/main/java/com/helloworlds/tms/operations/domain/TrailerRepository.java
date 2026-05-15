package com.helloworlds.tms.operations.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface TrailerRepository extends JpaRepository<Trailer, UUID> {

    @Query("""
        SELECT t FROM Trailer t
        WHERE (:companyId IS NULL OR t.companyId = :companyId)
          AND (:status    IS NULL OR t.status    = :status)
          AND (:q         IS NULL OR LOWER(COALESCE(t.nickname,'')) LIKE LOWER(CONCAT('%', :q, '%'))
                                  OR LOWER(COALESCE(t.vin,''))      LIKE LOWER(CONCAT('%', :q, '%'))
                                  OR LOWER(COALESCE(t.plateNumber,'')) LIKE LOWER(CONCAT('%', :q, '%')))
        ORDER BY t.nickname NULLS LAST, t.createdAt DESC
        """)
    Page<Trailer> search(@Param("companyId") UUID companyId,
                          @Param("status") String status,
                          @Param("q") String q,
                          Pageable pageable);
}
