package com.temple.platform.platform.api;

import com.temple.platform.platform.repository.ApplicationMetadataRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DatabaseStatusController.class)
@WithMockUser(roles = "PLATFORM_ADMIN")
class DatabaseStatusControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ApplicationMetadataRepository applicationMetadataRepository;

    @Test
    void databaseReturnsSchemaAndFlywayVersions() throws Exception {
        when(applicationMetadataRepository.findValue("schema_version")).thenReturn(Optional.of("6"));
        when(applicationMetadataRepository.findLatestFlywayVersion()).thenReturn(Optional.of("6"));

        mockMvc.perform(get("/api/v1/system/database"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.schemaVersion").value("6"))
                .andExpect(jsonPath("$.flywayVersion").value("6"));
    }
}
