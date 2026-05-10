package com.handa.tms.notification;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * Reads {@code *.notify.v1} Kafka events from other services and dispatches
 * email, SMS, push, or webhooks based on tenant preferences.  Phase-1 stub.
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class NotificationServiceApplication {
    public static void main(String[] args) { SpringApplication.run(NotificationServiceApplication.class, args); }
}
