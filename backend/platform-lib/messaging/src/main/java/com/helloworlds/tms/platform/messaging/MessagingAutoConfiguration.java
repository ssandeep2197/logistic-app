package com.helloworlds.tms.platform.messaging;

import com.helloworlds.tms.platform.messaging.outbox.OutboxDispatcher;
import com.helloworlds.tms.platform.messaging.outbox.OutboxEventRepository;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Wires the OutboxDispatcher.  Does NOT declare {@code @EnableJpaRepositories}
 * because doing so disables Spring Boot's default scanning of the service's
 * own packages.  Every consuming service must add to its main class:
 *
 * <pre>{@code
 * @EnableJpaRepositories(basePackages = {
 *     "com.helloworlds.tms.<your-service>",
 *     "com.helloworlds.tms.platform.messaging.outbox"
 * })
 * @EntityScan(basePackages = {
 *     "com.helloworlds.tms.<your-service>",
 *     "com.helloworlds.tms.platform.core.outbox"
 * })
 * }</pre>
 *
 * The platform-lib-core {@link com.helloworlds.tms.platform.core.outbox.OutboxEvent}
 * entity is mapped to the table {@code outbox_event} in the service's default
 * schema, so each service ends up with its own outbox table.
 */
@AutoConfiguration
@EnableScheduling
@ConditionalOnProperty(prefix = "tms.messaging.outbox", name = "enabled", havingValue = "true", matchIfMissing = true)
public class MessagingAutoConfiguration {

    @Bean
    @ConditionalOnBean({KafkaTemplate.class, OutboxEventRepository.class})
    public OutboxDispatcher outboxDispatcher(OutboxEventRepository repo,
                                              KafkaTemplate<String, String> kafka) {
        return new OutboxDispatcher(repo, kafka);
    }
}
