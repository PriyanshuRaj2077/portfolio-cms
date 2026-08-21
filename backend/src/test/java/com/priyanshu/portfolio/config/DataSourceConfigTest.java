package com.priyanshu.portfolio.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.Environment;
import org.springframework.test.util.ReflectionTestUtils;

import javax.sql.DataSource;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DataSourceConfigTest {

    @Test
    @DisplayName("DataSourceConfig fails fast in production when database environment variables are missing")
    void testProductionFailsFastWithoutPostgreSqlVariables() {
        DataSourceConfig config = new DataSourceConfig();
        ReflectionTestUtils.setField(config, "rawUrl", "");
        ReflectionTestUtils.setField(config, "rawUsername", "");
        ReflectionTestUtils.setField(config, "rawPassword", "");
        ReflectionTestUtils.setField(config, "driverClassName", "");

        Environment env = mock(Environment.class);
        when(env.getActiveProfiles()).thenReturn(new String[]{"prod"});

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> config.createHikariConfig(env));
        assertTrue(ex.getMessage().contains("FATAL PRODUCTION DATABASE CONFIGURATION"));
        assertTrue(ex.getMessage().contains("JDBC_DATABASE_URL"));
    }

    @Test
    @DisplayName("DataSourceConfig normalizes postgresql:// URI format into valid JDBC PostgreSQL URL")
    void testNormalizesCloudPostgreSqlUri() {
        DataSourceConfig config = new DataSourceConfig();
        ReflectionTestUtils.setField(config, "rawUrl", "postgresql://neondb_owner:npg_Secret123@ep-cool-fog-12345.us-east-2.aws.neon.tech:5432/neondb?sslmode=require");
        ReflectionTestUtils.setField(config, "rawUsername", "");
        ReflectionTestUtils.setField(config, "rawPassword", "");
        ReflectionTestUtils.setField(config, "driverClassName", "");

        Environment env = mock(Environment.class);
        when(env.getActiveProfiles()).thenReturn(new String[]{"prod"});

        HikariConfig hikariConfig = config.createHikariConfig(env);
        assertNotNull(hikariConfig);

        assertEquals("jdbc:postgresql://ep-cool-fog-12345.us-east-2.aws.neon.tech:5432/neondb?sslmode=require", hikariConfig.getJdbcUrl());
        assertEquals("neondb_owner", hikariConfig.getUsername());
        assertEquals("npg_Secret123", hikariConfig.getPassword());
        assertEquals("org.postgresql.Driver", hikariConfig.getDriverClassName());
    }

    @Test
    @DisplayName("DataSourceConfig preserves standard JDBC PostgreSQL URL in production")
    void testPreservesStandardJdbcUrl() {
        DataSourceConfig config = new DataSourceConfig();
        ReflectionTestUtils.setField(config, "rawUrl", "jdbc:postgresql://ep-cool-fog-12345.us-east-2.aws.neon.tech:5432/neondb?sslmode=require");
        ReflectionTestUtils.setField(config, "rawUsername", "neon_user");
        ReflectionTestUtils.setField(config, "rawPassword", "neon_pass");
        ReflectionTestUtils.setField(config, "driverClassName", "org.postgresql.Driver");

        Environment env = mock(Environment.class);
        when(env.getActiveProfiles()).thenReturn(new String[]{"prod"});

        HikariConfig hikariConfig = config.createHikariConfig(env);
        assertNotNull(hikariConfig);

        assertEquals("jdbc:postgresql://ep-cool-fog-12345.us-east-2.aws.neon.tech:5432/neondb?sslmode=require", hikariConfig.getJdbcUrl());
        assertEquals("neon_user", hikariConfig.getUsername());
        assertEquals("org.postgresql.Driver", hikariConfig.getDriverClassName());
    }

    @Test
    @DisplayName("DataSourceConfig configures H2 fallback for dev and test profiles")
    void testConfiguresH2ForDevAndTest() {
        DataSourceConfig config = new DataSourceConfig();
        ReflectionTestUtils.setField(config, "rawUrl", "");
        ReflectionTestUtils.setField(config, "rawUsername", "");
        ReflectionTestUtils.setField(config, "rawPassword", "");
        ReflectionTestUtils.setField(config, "driverClassName", "");

        Environment env = mock(Environment.class);
        when(env.getActiveProfiles()).thenReturn(new String[]{"dev"});

        DataSource ds = config.dataSource(env);
        assertNotNull(ds);
        assertInstanceOf(HikariDataSource.class, ds);

        HikariDataSource hikari = (HikariDataSource) ds;
        assertEquals("jdbc:h2:mem:portfoliodb;DB_CLOSE_DELAY=-1;MODE=PostgreSQL", hikari.getJdbcUrl());
        assertEquals("sa", hikari.getUsername());
        assertEquals("org.h2.Driver", hikari.getDriverClassName());
        hikari.close();
    }
}
