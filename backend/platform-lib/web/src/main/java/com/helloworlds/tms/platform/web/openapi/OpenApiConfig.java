package com.helloworlds.tms.platform.web.openapi;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Default OpenAPI document.  Each service overrides the title via
 * {@code spring.application.name}.  The bearer auth scheme is declared once
 * here so every endpoint shows the "Authorize" button in Swagger UI.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    @ConditionalOnMissingBean
    public OpenAPI tmsOpenApi() {
        SecurityScheme bearer = new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .description("Access token issued by identity-service");
        return new OpenAPI()
                .info(new Info()
                        .title("TMS service")
                        .version("v1")
                        .description("Auto-generated; replace via spring.application.name + custom OpenAPI bean."))
                .components(new Components().addSecuritySchemes("bearer", bearer))
                .addSecurityItem(new SecurityRequirement().addList("bearer"));
    }
}
