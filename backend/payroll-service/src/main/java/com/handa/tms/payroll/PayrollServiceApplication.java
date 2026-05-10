package com.handa.tms.payroll;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * Driver pay (mileage / percent / hourly / per-diem), deductions, settlements,
 * year-end W-2/1099.  Phase-1 stub; will use Spring Batch for the weekly run.
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class PayrollServiceApplication {
    public static void main(String[] args) { SpringApplication.run(PayrollServiceApplication.class, args); }
}
