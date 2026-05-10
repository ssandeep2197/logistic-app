package com.helloworlds.tms.finance;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * A/R invoices, payments, A/P bills, GL postings.  All money in BigDecimal —
 * NEVER double.  Phase-1 stub.
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class FinanceServiceApplication {
    public static void main(String[] args) { SpringApplication.run(FinanceServiceApplication.class, args); }
}
