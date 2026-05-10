package com.helloworlds.tms.identity.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PermissionRepository extends JpaRepository<Permission, UUID> {
    Optional<Permission> findByResourceAndActionAndScope(String resource, String action, String scope);
}
