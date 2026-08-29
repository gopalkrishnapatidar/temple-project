package com.temple.platform.identity.domain;

import java.time.OffsetDateTime;

public record Account(
        long id,
        String email,
        String passwordHash,
        AccountRole role,
        AccountStatus status,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
