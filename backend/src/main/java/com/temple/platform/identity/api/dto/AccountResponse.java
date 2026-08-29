package com.temple.platform.identity.api.dto;

import com.temple.platform.identity.domain.AccountRole;
import com.temple.platform.identity.domain.AccountStatus;

public record AccountResponse(
        long id,
        String email,
        AccountRole role,
        AccountStatus status
) {
}
