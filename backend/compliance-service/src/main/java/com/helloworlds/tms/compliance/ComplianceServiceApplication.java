package com.helloworlds.tms.compliance;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * Tax + safety compliance.  IFTA quarterly worksheets are computed from
 * tracking-service state-mile rows + fuel-purchase events; DOT Oregon WMT
 * uses similar inputs.  Phase-1 stub.
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class ComplianceServiceApplication {
    public static void main(String[] args) { SpringApplication.run(ComplianceServiceApplication.class, args); }
}
