package com.temple.platform.support;

import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Shared disposable PostgreSQL for Spring tests. One container per JVM;
 * connection details are applied at highest property-source precedence so
 * developer env vars cannot point tests at a persistent local database.
 */
public final class IsolatedPostgres {

    static final String PROPERTY_SOURCE_NAME = "isolatedPostgresContainer";

    private static final Object LOCK = new Object();
    private static volatile PostgreSQLContainer container;

    private IsolatedPostgres() {
    }

    public static PostgreSQLContainer container() {
        return ensureStarted();
    }

    static void applyTo(ConfigurableEnvironment environment) {
        if (environment.getPropertySources().contains(PROPERTY_SOURCE_NAME)) {
            return;
        }
        environment.getPropertySources().addFirst(
                new MapPropertySource(PROPERTY_SOURCE_NAME, datasourceProperties())
        );
    }

    static Map<String, Object> datasourceProperties() {
        PostgreSQLContainer postgres = ensureStarted();
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("spring.datasource.url", postgres.getJdbcUrl());
        properties.put("spring.datasource.username", postgres.getUsername());
        properties.put("spring.datasource.password", postgres.getPassword());
        properties.put("spring.flyway.url", postgres.getJdbcUrl());
        properties.put("spring.flyway.user", postgres.getUsername());
        properties.put("spring.flyway.password", postgres.getPassword());
        properties.put("SPRING_DATASOURCE_URL", postgres.getJdbcUrl());
        properties.put("SPRING_DATASOURCE_USERNAME", postgres.getUsername());
        properties.put("SPRING_DATASOURCE_PASSWORD", postgres.getPassword());
        properties.put("SPRING_FLYWAY_USERNAME", postgres.getUsername());
        properties.put("SPRING_FLYWAY_PASSWORD", postgres.getPassword());
        properties.put("NOTIFICATION_KAFKA_ENABLED", "false");
        properties.put("app.notification.kafka.enabled", "false");
        return properties;
    }

    private static PostgreSQLContainer ensureStarted() {
        PostgreSQLContainer existing = container;
        if (existing != null) {
            return existing;
        }
        synchronized (LOCK) {
            if (container == null) {
                PostgreSQLContainer postgres = new PostgreSQLContainer(DockerImageName.parse("postgres:16-alpine"))
                        .withDatabaseName("temple_platform_test")
                        .withUsername("temple_test")
                        .withPassword(UUID.randomUUID().toString());
                postgres.start();
                container = postgres;
            }
            return container;
        }
    }
}
