package com.priyanshu.portfolio.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;

import javax.sql.DataSource;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Configuration
public class DataSourceConfig {

    private static final Logger log = LoggerFactory.getLogger(DataSourceConfig.class);

    @Value("${spring.datasource.url:}")
    private String rawUrl;

    @Value("${spring.datasource.username:}")
    private String rawUsername;

    @Value("${spring.datasource.password:}")
    private String rawPassword;

    @Value("${spring.datasource.driver-class-name:}")
    private String driverClassName;

    public HikariConfig createHikariConfig(Environment environment) {
        String[] activeProfiles = environment != null ? environment.getActiveProfiles() : new String[0];
        boolean isDevOrTest = Arrays.stream(activeProfiles)
                .anyMatch(p -> p.equalsIgnoreCase("dev") || p.equalsIgnoreCase("test"));

        String normalizedUrl = rawUrl != null ? rawUrl.trim() : "";
        String resolvedUsername = rawUsername != null ? rawUsername.trim() : "";
        String resolvedPassword = rawPassword != null ? rawPassword.trim() : "";
        String resolvedDriver = driverClassName != null ? driverClassName.trim() : "";

        if (isDevOrTest) {
            // Local dev / test fallback
            if (normalizedUrl.isEmpty()) {
                normalizedUrl = "jdbc:h2:mem:portfoliodb;DB_CLOSE_DELAY=-1;MODE=PostgreSQL";
                resolvedUsername = "sa";
                resolvedPassword = "";
                resolvedDriver = "org.h2.Driver";
            }
        } else {
            // PRODUCTION: Strictly require Neon PostgreSQL configuration and fail fast
            List<String> missingVars = new ArrayList<>();
            if (normalizedUrl.isEmpty()) {
                missingVars.add("JDBC_DATABASE_URL");
            }

            boolean hasEmbeddedCredentials = (normalizedUrl.startsWith("postgres://") || normalizedUrl.startsWith("postgresql://"))
                    && normalizedUrl.contains("@");

            if (resolvedUsername.isEmpty() && !hasEmbeddedCredentials) {
                missingVars.add("JDBC_DATABASE_USERNAME");
            }

            if (resolvedPassword.isEmpty() && !hasEmbeddedCredentials) {
                missingVars.add("JDBC_DATABASE_PASSWORD");
            }

            if (!missingVars.isEmpty()) {
                throw new IllegalStateException(
                    "FATAL PRODUCTION DATABASE CONFIGURATION: Neon PostgreSQL is strictly required in production. " +
                    "H2 in-memory fallback is disallowed in production. Missing required database environment variables: " +
                    missingVars
                );
            }
        }

        // Handle postgres:// or postgresql:// URIs provided by cloud platforms (Render / Neon)
        if (normalizedUrl.startsWith("postgres://") || normalizedUrl.startsWith("postgresql://")) {
            try {
                String uriString = normalizedUrl.replaceFirst("^postgres(ql)?://", "http://");
                URI uri = new URI(uriString);

                String host = uri.getHost();
                int port = uri.getPort() != -1 ? uri.getPort() : 5432;
                String path = uri.getPath();
                String query = uri.getQuery();

                String userInfo = uri.getUserInfo();
                if (userInfo != null && !userInfo.isEmpty()) {
                    String[] parts = userInfo.split(":", 2);
                    if (resolvedUsername.isEmpty() && parts.length > 0) {
                        resolvedUsername = parts[0];
                    }
                    if (resolvedPassword.isEmpty() && parts.length > 1) {
                        resolvedPassword = parts[1];
                    }
                }

                StringBuilder jdbcUrl = new StringBuilder("jdbc:postgresql://")
                        .append(host)
                        .append(":")
                        .append(port)
                        .append(path);

                if (query != null && !query.isEmpty()) {
                    jdbcUrl.append("?").append(query);
                } else if (!jdbcUrl.toString().contains("sslmode=")) {
                    jdbcUrl.append("?sslmode=require");
                }

                normalizedUrl = jdbcUrl.toString();
                resolvedDriver = "org.postgresql.Driver";
            } catch (Exception e) {
                normalizedUrl = normalizedUrl.replaceFirst("^postgres(ql)?://", "jdbc:postgresql://");
                resolvedDriver = "org.postgresql.Driver";
            }
        }

        if (resolvedDriver.isEmpty()) {
            if (normalizedUrl.startsWith("jdbc:postgresql:")) {
                resolvedDriver = "org.postgresql.Driver";
            } else if (normalizedUrl.startsWith("jdbc:h2:")) {
                resolvedDriver = "org.h2.Driver";
            }
        }

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(normalizedUrl);
        config.setUsername(resolvedUsername);
        config.setPassword(resolvedPassword);

        if (!resolvedDriver.isEmpty()) {
            config.setDriverClassName(resolvedDriver);
        }

        // Production-tuned connection pool settings for Neon & PostgreSQL
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setConnectionTimeout(30000);
        config.setIdleTimeout(600000);
        config.setMaxLifetime(1800000);
        config.setPoolName("PortfolioHikariPool");

        return config;
    }

    @Bean
    @Primary
    public DataSource dataSource(Environment environment) {
        return new HikariDataSource(createHikariConfig(environment));
    }
}
