package com.helloworlds.tms.identity;

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
 * The {@code @EnableJpaRepositories} + {@code @EntityScan} include both
 * identity-service's own packages AND platform-lib-messaging's outbox
 * package — required because {@code MessagingAutoConfiguration} no longer
 * declares them itself (doing so would disable scanning of this service's
 * own repositories).
 */
@SpringBootApplication
@ConfigurationPropertiesScan
@EnableTransactionManagement
@EnableScheduling
@EnableAsync
@EnableKafka
@EnableJpaRepositories(basePackages = {
    "com.helloworlds.tms.identity",
    "com.helloworlds.tms.platform.messaging.outbox"
})
@EntityScan(basePackages = {
    "com.helloworlds.tms.identity",
    "com.helloworlds.tms.platform.core.outbox"
})
public class IdentityServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(IdentityServiceApplication.class, args);
    }
}
