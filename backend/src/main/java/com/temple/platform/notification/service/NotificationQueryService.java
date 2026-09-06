package com.temple.platform.notification.service;

import com.temple.platform.notification.api.dto.NotificationResponse;
import com.temple.platform.notification.domain.Notification;
import com.temple.platform.notification.repository.NotificationRepository;
import com.temple.platform.temple.api.dto.PageResponse;
import com.temple.platform.temple.exception.ResourceNotFoundException;
import com.temple.platform.temple.security.TempleAuthorizationService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static com.temple.platform.temple.api.PaginationSupport.PageRequest;
import static com.temple.platform.temple.api.PaginationSupport.resolve;

@Service
public class NotificationQueryService {

    private final NotificationRepository notificationRepository;
    private final TempleAuthorizationService authorizationService;

    public NotificationQueryService(
            NotificationRepository notificationRepository,
            TempleAuthorizationService authorizationService) {
        this.notificationRepository = notificationRepository;
        this.authorizationService = authorizationService;
    }

    @Transactional(readOnly = true)
    public PageResponse<NotificationResponse> list(Integer page, Integer size, Authentication authentication) {
        long accountId = authorizationService.requireAccountId(authentication);
        PageRequest pageRequest = resolve(page, size);
        List<Notification> notifications;
        long total;
        if (authorizationService.isPlatformAdmin(authentication)) {
            notifications = notificationRepository.findAll(pageRequest.size(), pageRequest.offset());
            total = notificationRepository.countAll();
        } else {
            notifications = notificationRepository.findByAccountId(
                    accountId,
                    pageRequest.size(),
                    pageRequest.offset()
            );
            total = notificationRepository.countByAccountId(accountId);
        }
        List<NotificationResponse> content = notifications.stream().map(NotificationQueryService::toResponse).toList();
        int totalPages = pageRequest.size() == 0 ? 0 : (int) Math.ceil((double) total / pageRequest.size());
        return new PageResponse<>(content, pageRequest.page(), pageRequest.size(), total, totalPages);
    }

    @Transactional(readOnly = true)
    public NotificationResponse get(UUID notificationReference, Authentication authentication) {
        Notification notification = notificationRepository.findByNotificationReference(notificationReference)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found"));
        if (!canAccess(notification, authentication)) {
            throw new ResourceNotFoundException("Notification not found");
        }
        return toResponse(notification);
    }

    private boolean canAccess(Notification notification, Authentication authentication) {
        long accountId = authorizationService.requireAccountId(authentication);
        if (notification.accountId() == accountId) {
            return true;
        }
        return authorizationService.isPlatformAdmin(authentication);
    }

    private static NotificationResponse toResponse(Notification notification) {
        return new NotificationResponse(
                notification.notificationReference(),
                notification.channel(),
                notification.type(),
                notification.status(),
                notification.title(),
                notification.message(),
                notification.createdAt(),
                notification.sentAt()
        );
    }
}
