package com.temple.platform.identity.api;

import com.temple.platform.identity.api.dto.AuthorizationProbeResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/internal")
public class InternalAuthorizationController {

    @GetMapping("/temple-admin")
    public AuthorizationProbeResponse templeAdmin() {
        return new AuthorizationProbeResponse("OK", "TEMPLE_ADMIN");
    }

    @GetMapping("/platform-admin")
    public AuthorizationProbeResponse platformAdmin() {
        return new AuthorizationProbeResponse("OK", "PLATFORM_ADMIN");
    }
}
