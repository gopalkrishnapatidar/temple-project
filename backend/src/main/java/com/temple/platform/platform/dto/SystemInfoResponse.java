package com.temple.platform.platform.dto;

import java.util.List;

public record SystemInfoResponse(
        String applicationName,
        String applicationVersion,
        List<String> activeProfiles
) {
}
