package com.temple.platform.identity.repository;

import com.temple.platform.identity.domain.AccountRole;
import com.temple.platform.identity.domain.AccountStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("dev")
class AccountRepositoryTest {

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @Transactional
    void uniqueNormalizedEmailIsEnforced() {
        String email = "dup-" + UUID.randomUUID() + "@example.com";
        accountRepository.insert(email, "hash-value-1", AccountRole.DEVOTEE, AccountStatus.ACTIVE);

        assertThatThrownBy(() ->
                accountRepository.insert(email, "hash-value-2", AccountRole.DEVOTEE, AccountStatus.ACTIVE)
        ).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @Transactional
    void invalidRoleIsRejected() {
        assertThatThrownBy(() -> jdbcTemplate.update(
                """
                INSERT INTO account (email, password_hash, role, status)
                VALUES (?, ?, ?, ?)
                """,
                "role-" + UUID.randomUUID() + "@example.com",
                "hash-value",
                "SUPERUSER",
                "ACTIVE"
        )).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @Transactional
    void blankPasswordHashIsRejected() {
        assertThatThrownBy(() -> jdbcTemplate.update(
                """
                INSERT INTO account (email, password_hash, role, status)
                VALUES (?, ?, ?, ?)
                """,
                "blank-" + UUID.randomUUID() + "@example.com",
                "   ",
                "DEVOTEE",
                "ACTIVE"
        )).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @Transactional
    void unnormalizedEmailIsRejected() {
        assertThatThrownBy(() -> jdbcTemplate.update(
                """
                INSERT INTO account (email, password_hash, role, status)
                VALUES (?, ?, ?, ?)
                """,
                "  Mixed.Case@Example.COM  ",
                "hash-value",
                "DEVOTEE",
                "ACTIVE"
        )).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void flywayAccountMigrationApplied() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM flyway_schema_history WHERE version = '4' AND success = TRUE",
                Integer.class
        );
        assertThat(count).isEqualTo(1);
    }
}
