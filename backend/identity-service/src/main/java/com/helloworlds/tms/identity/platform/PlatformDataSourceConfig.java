package com.helloworlds.tms.identity.platform;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

/**
 * Second Postgres connection used ONLY by the /platform/* endpoints.
 * <p>
 * The default {@code DataSource} that the rest of the service uses is bound
 * to the {@code identity_svc} role — that role does NOT have {@code BYPASSRLS},
 * so every query it issues is filtered by the per-tenant Row Level Security
 * policies.  That's the whole point: a bug in app code can't leak another
 * tenant's data.
 * <p>
 * Platform admin queries need to see ACROSS every tenant.  Rather than mix
 * privileges on one role (and risk a future change accidentally bypassing
 * RLS for ordinary code), this bean binds a SEPARATE connection pool to the
 * {@code tms} superuser role, which does have {@code BYPASSRLS}.  Only the
 * platform service uses it; everything else stays on the constrained pool.
 */
@Configuration
public class PlatformDataSourceConfig {

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
        // Tiny pool — these queries are interactive / on-demand.
        cfg.setMaximumPoolSize(4);
        cfg.setMinimumIdle(1);
        cfg.setConnectionTimeout(5_000);
        cfg.setReadOnly(true);              // Defense in depth: refuse writes.
        return new HikariDataSource(cfg);
    }

    @Bean(name = "platformJdbc")
    public JdbcTemplate platformJdbc(@Qualifier("platformDataSource") DataSource ds) {
        return new JdbcTemplate(ds);
    }
}
