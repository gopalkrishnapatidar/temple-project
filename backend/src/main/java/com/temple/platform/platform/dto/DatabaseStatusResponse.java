package com.temple.platform.platform.dto;

public record DatabaseStatusResponse(
        String schemaVersion,
        String flywayVersion
) {
}
