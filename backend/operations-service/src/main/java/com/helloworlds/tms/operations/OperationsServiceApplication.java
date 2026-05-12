package com.helloworlds.tms.operations;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * Owns Customer + Load.  Stop / Driver / Truck come later when v1 use cases
 * outgrow single-pickup-single-delivery + free-text driver names.
 * Issues load.* outbox events for downstream services (dispatch, finance,
 * reporting).
 * <p>
 * Both {@code @EnableJpaRepositories} and {@code @EntityScan} are explicit
 * because {@link com.helloworlds.tms.platform.messaging.MessagingAutoConfiguration}
 * deliberately does NOT declare them — see runtime-gotchas in memory.
 */
@SpringBootApplication
@ConfigurationPropertiesScan
@EnableTransactionManagement
@EnableScheduling
@EnableAsync
@EnableKafka
@EnableJpaRepositories(basePackages = {
    "com.helloworlds.tms.operations",
    "com.helloworlds.tms.platform.messaging.outbox"
})
@EntityScan(basePackages = {
    "com.helloworlds.tms.operations",
    "com.helloworlds.tms.platform.core.outbox"
})
public class OperationsServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(OperationsServiceApplication.class, args);
    }
}
