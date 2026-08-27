package com.temple.platform.platform.api;

import com.temple.platform.platform.dto.PingResponse;
import com.temple.platform.platform.dto.SystemInfoResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/api/v1/system")
public class SystemController {

    private final String applicationName;
    private final String applicationVersion;
    private final Environment environment;

    public SystemController(
            @Value("${spring.application.name}") String applicationName,
            @Value("${app.version:unknown}") String applicationVersion,
            Environment environment) {
        this.applicationName = applicationName;
        this.applicationVersion = applicationVersion;
        this.environment = environment;
    }

    @GetMapping("/ping")
    public PingResponse ping() {
        return new PingResponse("UP", "Temple Platform API is running");
    }

    @GetMapping("/info")
    public SystemInfoResponse info() {
        List<String> activeProfiles = Arrays.asList(environment.getActiveProfiles());
        return new SystemInfoResponse(applicationName, applicationVersion, activeProfiles);
    }
}
