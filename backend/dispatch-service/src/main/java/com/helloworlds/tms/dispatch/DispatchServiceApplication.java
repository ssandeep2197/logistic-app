package com.helloworlds.tms.dispatch;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * Owns load assignment and the auto-scheduler.  Reads truck positions from the
 * tracking-service projection (Kafka consumer) and load events from operations.
 * Phase-1 stub.
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class DispatchServiceApplication {
    public static void main(String[] args) { SpringApplication.run(DispatchServiceApplication.class, args); }
}
