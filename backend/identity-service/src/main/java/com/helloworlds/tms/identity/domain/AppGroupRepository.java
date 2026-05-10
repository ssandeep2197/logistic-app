package com.helloworlds.tms.identity.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AppGroupRepository extends JpaRepository<AppGroup, UUID> {
    Optional<AppGroup> findByName(String name);
}
