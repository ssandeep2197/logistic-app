package com.helloworlds.tms.identity.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface OAuthIdentityRepository extends JpaRepository<OAuthIdentity, UUID> {

    /**
     * Lookup by the IdP's immutable id.  Bypasses tenant-isolation because
     * the unique constraint is global — the caller switches into the right
     * tenant after this returns.
     */
    Optional<OAuthIdentity> findByProviderAndSubject(String provider, String subject);
}
