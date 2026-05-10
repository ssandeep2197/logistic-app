package com.helloworlds.tms.platform.security;

import com.helloworlds.tms.platform.security.rbac.PermissionEvaluator;
import com.helloworlds.tms.platform.security.rls.RlsGucInterceptor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Auto-configures the JWT security chain for any service that depends on
 * {@code platform-lib-security}.  Services can override {@code SecurityFilterChain}
 * if they need exotic behavior; in practice they almost never need to.
 */
@AutoConfiguration
@EnableConfigurationProperties({JwtProperties.class, SecurityProperties.class})
@Import({PermissionEvaluator.class, RlsGucInterceptor.class, SecurityExceptionHandler.class})
public class SecurityAutoConfiguration {

    @Bean
    public JwtService jwtService(JwtProperties props) {
        return new JwtService(props);
    }

    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter(JwtService jwt, SecurityProperties props) {
        return new JwtAuthenticationFilter(jwt, props);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                    JwtAuthenticationFilter jwtFilter,
                                                    SecurityProperties props) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .cors(cors -> {})        // Spring Boot wires CorsConfigurationSource if present.
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> {
                props.publicPaths().forEach(p -> auth.requestMatchers(p).permitAll());
                auth.requestMatchers(
                        "/actuator/health", "/actuator/health/**",
                        "/actuator/info",
                        "/v3/api-docs", "/v3/api-docs/**",
                        "/swagger-ui/**", "/swagger-ui.html"
                    ).permitAll();
                auth.anyRequest().authenticated();
            })
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
