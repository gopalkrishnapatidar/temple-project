package com.temple.platform.temple.service;

import com.temple.platform.cache.CacheInvalidationPublisher;
import com.temple.platform.cache.CacheKeys;
import com.temple.platform.cache.CacheProperties;
import com.temple.platform.cache.CatalogCache;
import com.temple.platform.cache.CatalogCacheTypeRefs;
import com.temple.platform.temple.api.dto.CreateTempleAdminAssignmentRequest;
import com.temple.platform.temple.api.dto.CreateTempleEventRequest;
import com.temple.platform.temple.api.dto.CreateTempleRequest;
import com.temple.platform.temple.api.dto.PageResponse;
import com.temple.platform.temple.api.dto.TempleEventResponse;
import com.temple.platform.temple.api.dto.TempleResponse;
import com.temple.platform.temple.api.dto.UpdateTempleEventRequest;
import com.temple.platform.temple.api.dto.UpdateTempleRequest;
import com.temple.platform.temple.domain.EventStatus;
import com.temple.platform.temple.domain.Temple;
import com.temple.platform.temple.domain.TempleEvent;
import com.temple.platform.temple.domain.TempleStatus;
import com.temple.platform.temple.exception.DuplicateAssignmentException;
import com.temple.platform.temple.exception.ForbiddenOperationException;
import com.temple.platform.temple.exception.InvalidEventScheduleException;
import com.temple.platform.temple.exception.InvalidEventStatusTransitionException;
import com.temple.platform.temple.exception.ResourceNotFoundException;
import com.temple.platform.temple.repository.TempleAdminAssignmentRepository;
import com.temple.platform.temple.repository.TempleEventRepository;
import com.temple.platform.temple.repository.TempleRepository;
import com.temple.platform.temple.security.TempleAuthorizationService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;

import static com.temple.platform.temple.api.PaginationSupport.PageRequest;
import static com.temple.platform.temple.api.PaginationSupport.resolve;

@Service
public class TempleService {

    private final TempleRepository templeRepository;
    private final TempleAdminAssignmentRepository assignmentRepository;
    private final TempleEventRepository eventRepository;
    private final TempleAuthorizationService authorizationService;
    private final CatalogCache catalogCache;
    private final CacheProperties cacheProperties;
    private final CacheInvalidationPublisher cacheInvalidation;

    public TempleService(
            TempleRepository templeRepository,
            TempleAdminAssignmentRepository assignmentRepository,
            TempleEventRepository eventRepository,
            TempleAuthorizationService authorizationService,
            CatalogCache catalogCache,
            CacheProperties cacheProperties,
            CacheInvalidationPublisher cacheInvalidation) {
        this.templeRepository = templeRepository;
        this.assignmentRepository = assignmentRepository;
        this.eventRepository = eventRepository;
        this.authorizationService = authorizationService;
        this.catalogCache = catalogCache;
        this.cacheProperties = cacheProperties;
        this.cacheInvalidation = cacheInvalidation;
    }

    @Transactional
    public TempleResponse createTemple(CreateTempleRequest request, Authentication authentication) {
        authorizationService.requirePlatformAdmin(authentication);
        validateTimezone(request.timezone());
        Temple temple = templeRepository.insert(
                request.name().trim(),
                normalizeOptional(request.description()),
                request.city().trim(),
                normalizeOptional(request.state()),
                request.country().trim(),
                request.timezone().trim(),
                request.status()
        );
        cacheInvalidation.invalidateAfterCommit(
                CacheKeys.templeId(temple.id()),
                CacheKeys.publicTempleList());
        return toResponse(temple);
    }

    @Transactional(readOnly = true)
    public List<TempleResponse> listTemples(Authentication authentication) {
        long accountId = authorizationService.requireAccountId(authentication);
        boolean platformAdmin = authorizationService.isPlatformAdmin(authentication);
        boolean templeAdmin = authorizationService.isTempleAdmin(authentication);
        List<Temple> temples;
        if (platformAdmin) {
            temples = templeRepository.findAllVisible(true, accountId);
        } else if (templeAdmin) {
            temples = templeRepository.findAllVisible(false, accountId);
        } else {
            temples = catalogCache.getOrLoad(
                    CacheKeys.publicTempleList(),
                    CatalogCacheTypeRefs.TEMPLE_LIST,
                    cacheProperties.getPublicTempleListTtl(),
                    () -> templeRepository.findAllVisible(false, 0));
        }
        return temples.stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public TempleResponse getTemple(long templeId, Authentication authentication) {
        Temple temple = catalogCache.getOrLoad(
                CacheKeys.templeId(templeId),
                Temple.class,
                cacheProperties.getTempleIdTtl(),
                () -> templeRepository.findById(templeId)
                        .orElseThrow(() -> new ResourceNotFoundException("Temple not found")));
        if (!canViewTemple(temple, authentication)) {
            throw new ResourceNotFoundException("Temple not found");
        }
        return toResponse(temple);
    }

    @Transactional
    public TempleResponse updateTemple(long templeId, UpdateTempleRequest request, Authentication authentication) {
        authorizationService.requireTempleManagement(authentication, templeId);
        Temple existing = templeRepository.findById(templeId)
                .orElseThrow(() -> new ResourceNotFoundException("Temple not found"));
        if (request.timezone() != null) {
            validateTimezone(request.timezone());
        }
        TempleRepository.UpdateTempleFields fields = new TempleRepository.UpdateTempleFields(
                trimToNull(request.name()),
                request.description() == null ? null : normalizeOptional(request.description()),
                trimToNull(request.city()),
                request.state() == null ? null : normalizeOptional(request.state()),
                trimToNull(request.country()),
                request.timezone() == null ? null : request.timezone().trim(),
                request.status()
        );
        if (!templeRepository.update(templeId, fields)) {
            throw new ResourceNotFoundException("Temple not found");
        }
        cacheInvalidation.invalidateAfterCommit(
                CacheKeys.templeId(templeId),
                CacheKeys.publicTempleList());
        return toResponse(templeRepository.findById(templeId).orElse(existing));
    }

    @Transactional
    public void assignTempleAdmin(long templeId, CreateTempleAdminAssignmentRequest request, Authentication authentication) {
        authorizationService.requireAssignmentManagement(authentication);
        templeRepository.findById(templeId)
                .orElseThrow(() -> new ResourceNotFoundException("Temple not found"));
        authorizationService.requireAssignableTempleAdminAccount(request.accountId());
        if (assignmentRepository.exists(request.accountId(), templeId)) {
            throw new DuplicateAssignmentException();
        }
        try {
            assignmentRepository.insert(request.accountId(), templeId);
        } catch (DataIntegrityViolationException ex) {
            throw new DuplicateAssignmentException();
        }
    }

    @Transactional
    public void removeTempleAdmin(long templeId, long accountId, Authentication authentication) {
        authorizationService.requireAssignmentManagement(authentication);
        templeRepository.findById(templeId)
                .orElseThrow(() -> new ResourceNotFoundException("Temple not found"));
        if (!assignmentRepository.delete(accountId, templeId)) {
            throw new ResourceNotFoundException("Temple admin assignment not found");
        }
    }

    @Transactional
    public TempleEventResponse createEvent(long templeId, CreateTempleEventRequest request, Authentication authentication) {
        authorizationService.requireTempleManagement(authentication, templeId);
        templeRepository.findById(templeId)
                .orElseThrow(() -> new ResourceNotFoundException("Temple not found"));
        validateSchedule(request.startAt(), request.endAt());
        TempleEvent event = eventRepository.insert(
                templeId,
                request.name().trim(),
                normalizeOptional(request.description()),
                request.startAt(),
                request.endAt(),
                EventStatus.DRAFT
        );
        cacheInvalidation.invalidateAfterCommit(CacheKeys.eventId(event.id()));
        return toEventResponse(event);
    }

    @Transactional(readOnly = true)
    public PageResponse<TempleEventResponse> listEvents(
            long templeId,
            Integer page,
            Integer size,
            Authentication authentication) {
        Temple temple = templeRepository.findById(templeId)
                .orElseThrow(() -> new ResourceNotFoundException("Temple not found"));
        if (!canViewTemple(temple, authentication)) {
            throw new ResourceNotFoundException("Temple not found");
        }
        PageRequest pageRequest = resolve(page, size);
        boolean adminView = canManageTemple(templeId, authentication);
        List<TempleEventResponse> content = eventRepository
                .findByTempleId(templeId, adminView, pageRequest.size(), pageRequest.offset())
                .stream()
                .map(this::toEventResponse)
                .toList();
        long total = eventRepository.countByTempleId(templeId, adminView);
        int totalPages = pageRequest.size() == 0 ? 0 : (int) Math.ceil((double) total / pageRequest.size());
        return new PageResponse<>(content, pageRequest.page(), pageRequest.size(), total, totalPages);
    }

    @Transactional(readOnly = true)
    public TempleEventResponse getEvent(long templeId, long eventId, Authentication authentication) {
        Temple temple = templeRepository.findById(templeId)
                .orElseThrow(() -> new ResourceNotFoundException("Temple not found"));
        if (!canViewTemple(temple, authentication)) {
            throw new ResourceNotFoundException("Temple not found");
        }
        TempleEvent event = catalogCache.getOrLoad(
                CacheKeys.eventId(eventId),
                TempleEvent.class,
                cacheProperties.getEventIdTtl(),
                () -> eventRepository.findByTempleIdAndId(templeId, eventId)
                        .orElseThrow(() -> new ResourceNotFoundException("Event not found")));
        if (!canViewEvent(event, templeId, authentication)) {
            throw new ResourceNotFoundException("Event not found");
        }
        return toEventResponse(event);
    }

    @Transactional
    public TempleEventResponse updateEvent(
            long templeId,
            long eventId,
            UpdateTempleEventRequest request,
            Authentication authentication) {
        authorizationService.requireTempleManagement(authentication, templeId);
        TempleEvent existing = eventRepository.findByTempleIdAndId(templeId, eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found"));
        OffsetDateTime startAt = request.startAt() != null ? request.startAt() : existing.startAt();
        OffsetDateTime endAt = request.endAt() != null ? request.endAt() : existing.endAt();
        validateSchedule(startAt, endAt);
        if (request.status() != null) {
            validateStatusTransition(existing.status(), request.status());
        }
        TempleEventRepository.UpdateEventFields fields = new TempleEventRepository.UpdateEventFields(
                trimToNull(request.name()),
                request.description() == null ? null : normalizeOptional(request.description()),
                request.startAt(),
                request.endAt(),
                request.status()
        );
        if (!eventRepository.update(templeId, eventId, fields)) {
            throw new ResourceNotFoundException("Event not found");
        }
        cacheInvalidation.invalidateAfterCommit(CacheKeys.eventId(eventId));
        return toEventResponse(eventRepository.findByTempleIdAndId(templeId, eventId).orElse(existing));
    }

    private boolean canViewTemple(Temple temple, Authentication authentication) {
        if (authorizationService.isPlatformAdmin(authentication)) {
            return true;
        }
        long accountId = authorizationService.requireAccountId(authentication);
        if (authorizationService.isTempleAdmin(authentication)) {
            return assignmentRepository.exists(accountId, temple.id());
        }
        return temple.status() == TempleStatus.ACTIVE;
    }

    private boolean canManageTemple(long templeId, Authentication authentication) {
        if (authorizationService.isPlatformAdmin(authentication)) {
            return true;
        }
        if (authorizationService.isTempleAdmin(authentication)) {
            long accountId = authorizationService.requireAccountId(authentication);
            return assignmentRepository.exists(accountId, templeId);
        }
        return false;
    }

    private boolean canViewEvent(TempleEvent event, long templeId, Authentication authentication) {
        if (canManageTemple(templeId, authentication)) {
            return true;
        }
        return event.status() == EventStatus.PUBLISHED;
    }

    private static void validateSchedule(OffsetDateTime startAt, OffsetDateTime endAt) {
        if (startAt == null || endAt == null || !endAt.isAfter(startAt)) {
            throw new InvalidEventScheduleException();
        }
    }

    private static void validateStatusTransition(EventStatus current, EventStatus requested) {
        if (current == requested) {
            return;
        }
        boolean allowed = switch (current) {
            case DRAFT -> requested == EventStatus.PUBLISHED || requested == EventStatus.CANCELLED;
            case PUBLISHED -> requested == EventStatus.CANCELLED;
            case CANCELLED -> false;
        };
        if (!allowed) {
            throw new InvalidEventStatusTransitionException();
        }
    }

    private static void validateTimezone(String timezone) {
        ZoneId.of(timezone.trim());
    }

    private static String normalizeOptional(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private TempleResponse toResponse(Temple temple) {
        return new TempleResponse(
                temple.id(),
                temple.name(),
                temple.description(),
                temple.city(),
                temple.state(),
                temple.country(),
                temple.timezone(),
                temple.status(),
                temple.createdAt(),
                temple.updatedAt()
        );
    }

    private TempleEventResponse toEventResponse(TempleEvent event) {
        return new TempleEventResponse(
                event.id(),
                event.templeId(),
                event.name(),
                event.description(),
                event.startAt(),
                event.endAt(),
                event.status(),
                event.createdAt(),
                event.updatedAt()
        );
    }
}
