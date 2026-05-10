package com.handa.tms.platform.test;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Shared Postgres+TimescaleDB+PostGIS container used by every integration test.
 * Reuse is enabled so a fresh JVM finds an existing container and skips the
 * 10-second cold start — set {@code testcontainers.reuse.enable=true} in
 * {@code ~/.testcontainers.properties} to opt in.
 */
public final class TmsPostgresExtension {

    private static final PostgreSQLContainer<?> CONTAINER =
        new PostgreSQLContainer<>(DockerImageName.parse("timescale/timescaledb-ha:pg16")
                .asCompatibleSubstituteFor("postgres"))
            .withDatabaseName("tms_test")
            .withUsername("tms")
            .withPassword("tms_test_pw")
            .withReuse(true);

    static {
        CONTAINER.start();
    }

    private TmsPostgresExtension() {}

    public static void register(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", CONTAINER::getJdbcUrl);
        r.add("spring.datasource.username", CONTAINER::getUsername);
        r.add("spring.datasource.password", CONTAINER::getPassword);
        r.add("spring.jpa.hibernate.ddl-auto", () -> "none");
    }
}
