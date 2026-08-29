package com.temple.platform.platform.api;

import com.temple.platform.platform.dto.DatabaseStatusResponse;
import com.temple.platform.platform.repository.ApplicationMetadataRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/system")
public class DatabaseStatusController {

    private final ApplicationMetadataRepository applicationMetadataRepository;

    public DatabaseStatusController(ApplicationMetadataRepository applicationMetadataRepository) {
        this.applicationMetadataRepository = applicationMetadataRepository;
    }

    @GetMapping("/database")
    public DatabaseStatusResponse database() {
        String schemaVersion = applicationMetadataRepository.findValue("schema_version").orElse("unknown");
        String flywayVersion = applicationMetadataRepository.findLatestFlywayVersion().orElse("unknown");
        return new DatabaseStatusResponse(schemaVersion, flywayVersion);
    }
}
