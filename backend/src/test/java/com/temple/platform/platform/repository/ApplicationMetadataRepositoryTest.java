package com.temple.platform.platform.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("dev")
class ApplicationMetadataRepositoryTest {

    @Autowired
    private ApplicationMetadataRepository applicationMetadataRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Test
    void schemaVersionIsNineAfterFlyway() {
        assertThat(applicationMetadataRepository.findValue("schema_version")).contains("9");
        assertThat(applicationMetadataRepository.findLatestFlywayVersion()).contains("9");
    }

    @Test
    @Transactional
    void uniqueKeyConstraintRejectsDuplicates() {
        String key = "test_unique_" + UUID.randomUUID();
        applicationMetadataRepository.insert(key, "one");

        assertThatThrownBy(() -> applicationMetadataRepository.insert(key, "two"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @Transactional
    void blankValueCheckConstraintIsEnforced() {
        assertThatThrownBy(() -> applicationMetadataRepository.insert("test_blank_" + UUID.randomUUID(), "   "))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @Transactional
    void updatedAtTriggerChangesOnUpdate() {
        String key = "test_updated_at_" + UUID.randomUUID();
        applicationMetadataRepository.insert(key, "before");
        OffsetDateTime original = applicationMetadataRepository.findUpdatedAt(key);

        jdbcTemplate.update("UPDATE application_metadata SET value = ? WHERE key = ?", "after", key);

        OffsetDateTime afterUpdate = applicationMetadataRepository.findUpdatedAt(key);
        assertThat(afterUpdate).isAfter(original);
        assertThat(applicationMetadataRepository.findValue(key)).contains("after");
    }

    @Test
    void transactionRollbackDoesNotPersistInsert() {
        String key = "test_rollback_" + UUID.randomUUID();

        transactionTemplate.executeWithoutResult(status -> {
            applicationMetadataRepository.insert(key, "should-not-commit");
            status.setRollbackOnly();
        });

        assertThat(applicationMetadataRepository.findValue(key)).isEmpty();
    }
}
