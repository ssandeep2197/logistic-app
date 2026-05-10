package com.handa.tms.platform.web;

import com.handa.tms.platform.web.error.GlobalExceptionHandler;
import com.handa.tms.platform.web.openapi.OpenApiConfig;
import com.handa.tms.platform.web.request.RequestIdFilter;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.Ordered;

@AutoConfiguration
@Import({GlobalExceptionHandler.class, OpenApiConfig.class})
public class WebAutoConfiguration {

    @Bean
    public FilterRegistrationBean<RequestIdFilter> requestIdFilter() {
        FilterRegistrationBean<RequestIdFilter> reg = new FilterRegistrationBean<>(new RequestIdFilter());
        // Run before Spring Security so the request id is in MDC for auth failures too.
        reg.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return reg;
    }
}
