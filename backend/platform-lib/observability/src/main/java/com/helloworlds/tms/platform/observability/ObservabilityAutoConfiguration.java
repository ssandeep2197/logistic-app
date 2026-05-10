package com.helloworlds.tms.platform.observability;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.config.MeterFilter;
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;

/**
 * Adds {@code service.name} as a default tag on every metric so a Prometheus
 * scrape from a multi-service node can be split per service. Tracing
 * propagation is configured via spring-cloud-sleuth equivalents in
 * {@code micrometer-tracing}; the OTLP exporter activates when
 * {@code OTEL_EXPORTER_OTLP_ENDPOINT} is set.
 */
@AutoConfiguration
public class ObservabilityAutoConfiguration {

    @Bean
    public MeterRegistryCustomizer<MeterRegistry> commonTagsCustomizer(
            org.springframework.core.env.Environment env) {
        String svc = env.getProperty("spring.application.name", "tms-service");
        return registry -> registry.config().meterFilter(MeterFilter.commonTags(
                java.util.Arrays.asList(io.micrometer.core.instrument.Tag.of("service", svc))));
    }
}
