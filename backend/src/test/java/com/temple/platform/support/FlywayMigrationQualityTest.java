package com.temple.platform.support;

import com.temple.platform.platform.repository.ApplicationMetadataRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;

import static org.assertj.core.api.Assertions.assertThat;

@IsolatedPostgresIntegrationTest
class FlywayMigrationQualityTest {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ApplicationMetadataRepository applicationMetadataRepository;

    @Test
    void datasourceUsesDisposablePostgresContainerNotDeveloperDatabase() throws Exception {
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();
            assertThat(metaData.getURL()).contains("jdbc:postgresql://");
            assertThat(metaData.getURL()).doesNotContain("temple_platform_dev");
            assertThat(connection.getCatalog()).isEqualTo("temple_platform_test");
            assertThat(IsolatedPostgres.container().isRunning()).isTrue();
        }
    }

    @Test
    void flywayMigrationsReachVersion10() {
        String version = jdbcTemplate.queryForObject(
                """
                SELECT version
                FROM flyway_schema_history
                WHERE success = TRUE
                  AND version IS NOT NULL
                ORDER BY installed_rank DESC
                LIMIT 1
                """,
                String.class
        );

        assertThat(version).isEqualTo("10");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM flyway_schema_history WHERE success = FALSE",
                Integer.class
        )).isZero();
        assertThat(applicationMetadataRepository.findValue("schema_version")).contains("10");
        assertThat(applicationMetadataRepository.findLatestFlywayVersion()).contains("10");
    }

    @Test
    void module14OutboxAndNotificationUniquenessConstraintsExist() {
        assertThat(constraintExists("outbox_event_event_id_unique")).isTrue();
        assertThat(constraintExists("notification_source_event_id_unique")).isTrue();
    }

    private boolean constraintExists(String constraintName) {
        Integer count = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM pg_constraint
                WHERE conname = ?
                """,
                Integer.class,
                constraintName
        );
        return count != null && count == 1;
    }
}
