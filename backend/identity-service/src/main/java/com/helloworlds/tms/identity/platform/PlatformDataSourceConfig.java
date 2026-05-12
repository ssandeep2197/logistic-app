package com.helloworlds.tms.identity.platform;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

/**
 * Two Postgres connections — one for the app's regular per-tenant code,
 * one for /platform/* cross-tenant queries.
 * <p>
 * Declaring ANY {@code DataSource} bean disables Spring Boot's default
 * {@code DataSourceAutoConfiguration} ({@code @ConditionalOnMissingBean}),
 * so once we add the platform pool we have to wire the regular one ourselves
 * too.  Marking it {@code @Primary} keeps Liquibase + JPA pointed at the
 * tenant-isolated pool by default; the platform service explicitly asks
 * for the bypass pool via {@code @Qualifier("platformDataSource")}.
 * <p>
 * Roles:
 *   - <b>default DataSource</b> binds as {@code identity_svc} — RLS applies.
 *   - <b>platformDataSource</b> binds as {@code tms} (BYPASSRLS) and is
 *     {@code readOnly}, so even an accidental write from /platform code
 *     can't escape the per-tenant boundary.
 */
@Configuration
public class PlatformDataSourceConfig {

    /** Backs the default (identity_svc) DataSource via {@code spring.datasource.*}. */
    @Primary
    @Bean
    @ConfigurationProperties("spring.datasource")
    public DataSourceProperties tenantDataSourceProperties() {
        return new DataSourceProperties();
    }

    /** Default DataSource — RLS-enforced.  Used by Liquibase, JPA, everything. */
    @Primary
    @Bean
    public HikariDataSource dataSource(DataSourceProperties props) {
        HikariDataSource ds = props.initializeDataSourceBuilder()
                .type(HikariDataSource.class)
                .build();
        ds.setPoolName("tms-tenant");
        ds.setMaximumPoolSize(20);
        ds.setMinimumIdle(4);
        ds.setConnectionTimeout(5_000);
        return ds;
    }

    /** Cross-tenant read-only pool — BYPASSRLS via the {@code tms} superuser. */
    @Bean(name = "platformDataSource", destroyMethod = "close")
    public DataSource platformDataSource(
            @Value("${tms.platform.datasource.url}")      String url,
            @Value("${tms.platform.datasource.username}") String username,
            @Value("${tms.platform.datasource.password}") String password) {
        HikariConfig cfg = new HikariConfig();
        cfg.setJdbcUrl(url);
        cfg.setUsername(username);
        cfg.setPassword(password);
        cfg.setPoolName("tms-platform-readonly");
        cfg.setMaximumPoolSize(4);
        cfg.setMinimumIdle(1);
        cfg.setConnectionTimeout(5_000);
        cfg.setReadOnly(true);              // Refuses writes — defense in depth.
        return new HikariDataSource(cfg);
    }

    @Bean(name = "platformJdbc")
    public JdbcTemplate platformJdbc(@Qualifier("platformDataSource") DataSource ds) {
        return new JdbcTemplate(ds);
    }
}
