package com.temple.platform.notification.api;

import com.temple.platform.notification.api.dto.NotificationResponse;
import com.temple.platform.notification.service.NotificationQueryService;
import com.temple.platform.temple.api.dto.PageResponse;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {

    private final NotificationQueryService notificationQueryService;

    public NotificationController(NotificationQueryService notificationQueryService) {
        this.notificationQueryService = notificationQueryService;
    }

    @GetMapping
    public PageResponse<NotificationResponse> list(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            Authentication authentication) {
        return notificationQueryService.list(page, size, authentication);
    }

    @GetMapping("/{notificationReference}")
    public NotificationResponse get(
            @PathVariable UUID notificationReference,
            Authentication authentication) {
        return notificationQueryService.get(notificationReference, authentication);
    }
}
