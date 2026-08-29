package com.temple.platform.identity.repository;

import com.temple.platform.identity.domain.Account;
import com.temple.platform.identity.domain.AccountRole;
import com.temple.platform.identity.domain.AccountStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.time.OffsetDateTime;
import java.util.Optional;

@Repository
public class AccountRepository {

    private static final RowMapper<Account> ACCOUNT_ROW_MAPPER = (rs, rowNum) -> new Account(
            rs.getLong("id"),
            rs.getString("email"),
            rs.getString("password_hash"),
            AccountRole.valueOf(rs.getString("role")),
            AccountStatus.valueOf(rs.getString("status")),
            rs.getObject("created_at", OffsetDateTime.class),
            rs.getObject("updated_at", OffsetDateTime.class)
    );

    private final JdbcTemplate jdbcTemplate;

    public AccountRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Optional<Account> findById(long id) {
        return jdbcTemplate.query(
                """
                SELECT id, email, password_hash, role, status, created_at, updated_at
                FROM account
                WHERE id = ?
                """,
                ACCOUNT_ROW_MAPPER,
                id
        ).stream().findFirst();
    }

    public Optional<Account> findByEmail(String email) {
        return jdbcTemplate.query(
                """
                SELECT id, email, password_hash, role, status, created_at, updated_at
                FROM account
                WHERE email = ?
                """,
                ACCOUNT_ROW_MAPPER,
                email
        ).stream().findFirst();
    }

    public Account insert(String email, String passwordHash, AccountRole role, AccountStatus status) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                    """
                    INSERT INTO account (email, password_hash, role, status)
                    VALUES (?, ?, ?, ?)
                    """,
                    new String[] {"id"}
            );
            ps.setString(1, email);
            ps.setString(2, passwordHash);
            ps.setString(3, role.name());
            ps.setString(4, status.name());
            return ps;
        }, keyHolder);

        Number key = keyHolder.getKey();
        if (key == null) {
            throw new IllegalStateException("Account insert did not return an id");
        }
        return findById(key.longValue()).orElseThrow(() -> new IllegalStateException("Inserted account not found"));
    }
}
