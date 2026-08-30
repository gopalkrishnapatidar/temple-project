package com.temple.platform.temple.repository;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class TempleAdminAssignmentRepository {

    private final JdbcTemplate jdbcTemplate;

    public TempleAdminAssignmentRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public boolean exists(long accountId, long templeId) {
        Integer count = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM temple_admin_assignment
                WHERE account_id = ? AND temple_id = ?
                """,
                Integer.class,
                accountId,
                templeId
        );
        return count != null && count > 0;
    }

    public void insert(long accountId, long templeId) {
        try {
            jdbcTemplate.update(
                    """
                    INSERT INTO temple_admin_assignment (account_id, temple_id)
                    VALUES (?, ?)
                    """,
                    accountId,
                    templeId
            );
        } catch (DataIntegrityViolationException ex) {
            throw ex;
        }
    }

    public boolean delete(long accountId, long templeId) {
        return jdbcTemplate.update(
                """
                DELETE FROM temple_admin_assignment
                WHERE account_id = ? AND temple_id = ?
                """,
                accountId,
                templeId
        ) == 1;
    }
}
