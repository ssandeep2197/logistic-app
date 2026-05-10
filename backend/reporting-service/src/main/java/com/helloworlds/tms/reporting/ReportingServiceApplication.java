package com.helloworlds.tms.reporting;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * Canned reports (gross/profit/loads-by-employee, daily/weekly/monthly/qtrly/yearly)
 * and the custom report builder (jOOQ-backed safe dynamic SQL).  Reads from a
 * Postgres read replica + Kafka projection tables to keep OLTP unaffected.
 * Phase-1 stub.
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class ReportingServiceApplication {
    public static void main(String[] args) { SpringApplication.run(ReportingServiceApplication.class, args); }
}
