package com.helloworlds.tms.operations;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * Loads, stops, customers, carriers, lanes.  Owns the canonical "load"
 * aggregate; other services consume {@code load.*.v1} topics for projections.
 * <p>
 * Phase-1 stub.  Domain implementation lives in {@code com.helloworlds.tms.operations.load},
 * {@code .customer}, {@code .carrier} packages — each with domain/service/web layers.
 */
@SpringBootApplication
@ConfigurationPropertiesScan
@EnableTransactionManagement
@EnableScheduling
@EnableKafka
public class OperationsServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(OperationsServiceApplication.class, args);
    }
}
