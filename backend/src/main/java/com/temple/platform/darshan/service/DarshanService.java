package com.temple.platform.darshan.service;

import com.temple.platform.cache.CacheInvalidationPublisher;
import com.temple.platform.cache.CacheKeys;
import com.temple.platform.cache.CacheProperties;
import com.temple.platform.cache.CatalogCache;
import com.temple.platform.cache.CatalogCacheTypeRefs;
import com.temple.platform.booking.exception.SlotCapacityBelowConfirmedBookingsException;
import com.temple.platform.darshan.api.SlotQuerySupport;
import com.temple.platform.darshan.api.dto.CreateDarshanRequest;
import com.temple.platform.darshan.api.dto.CreateDarshanSlotRequest;
import com.temple.platform.darshan.api.dto.DarshanResponse;
import com.temple.platform.darshan.api.dto.DarshanSlotResponse;
import com.temple.platform.darshan.api.dto.UpdateDarshanRequest;
import com.temple.platform.darshan.api.dto.UpdateDarshanSlotRequest;
import com.temple.platform.darshan.domain.Darshan;
import com.temple.platform.darshan.domain.DarshanSlot;
import com.temple.platform.darshan.domain.DarshanSlotStatus;
import com.temple.platform.darshan.domain.DarshanStatus;
import com.temple.platform.darshan.exception.InvalidSlotCapacityException;
import com.temple.platform.darshan.exception.InvalidSlotScheduleException;
import com.temple.platform.darshan.exception.InvalidSlotStatusTransitionException;
import com.temple.platform.darshan.exception.OverlappingSlotException;
import com.temple.platform.booking.repository.BookingRepository;
import com.temple.platform.darshan.repository.DarshanRepository;
import com.temple.platform.darshan.repository.DarshanSlotRepository;
import com.temple.platform.temple.api.dto.PageResponse;
import com.temple.platform.temple.domain.Temple;
import com.temple.platform.temple.domain.TempleStatus;
import com.temple.platform.temple.exception.ResourceNotFoundException;
import com.temple.platform.temple.repository.TempleAdminAssignmentRepository;
import com.temple.platform.temple.repository.TempleRepository;
import com.temple.platform.temple.security.TempleAuthorizationService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

import static com.temple.platform.temple.api.PaginationSupport.PageRequest;
import static com.temple.platform.temple.api.PaginationSupport.resolve;

@Service
public class DarshanService {

    private final TempleRepository templeRepository;
    private final TempleAdminAssignmentRepository assignmentRepository;
    private final DarshanRepository darshanRepository;
    private final DarshanSlotRepository slotRepository;
    private final BookingRepository bookingRepository;
    private final TempleAuthorizationService authorizationService;
    private final CatalogCache catalogCache;
    private final CacheProperties cacheProperties;
    private final CacheInvalidationPublisher cacheInvalidation;

    public DarshanService(
            TempleRepository templeRepository,
            TempleAdminAssignmentRepository assignmentRepository,
            DarshanRepository darshanRepository,
            DarshanSlotRepository slotRepository,
            BookingRepository bookingRepository,
            TempleAuthorizationService authorizationService,
            CatalogCache catalogCache,
            CacheProperties cacheProperties,
            CacheInvalidationPublisher cacheInvalidation) {
        this.templeRepository = templeRepository;
        this.assignmentRepository = assignmentRepository;
        this.darshanRepository = darshanRepository;
        this.slotRepository = slotRepository;
        this.bookingRepository = bookingRepository;
        this.authorizationService = authorizationService;
        this.catalogCache = catalogCache;
        this.cacheProperties = cacheProperties;
        this.cacheInvalidation = cacheInvalidation;
    }

    @Transactional
    public DarshanResponse createDarshan(
            long templeId,
            CreateDarshanRequest request,
            Authentication authentication) {
        authorizationService.requireTempleManagement(authentication, templeId);
        requireTempleExists(templeId);
        Darshan darshan = darshanRepository.insert(
                templeId,
                request.name().trim(),
                normalizeOptional(request.description()),
                DarshanStatus.ACTIVE
        );
        cacheInvalidation.invalidateAfterCommit(
                CacheKeys.darshanId(darshan.id()),
                CacheKeys.publicDarshanList(templeId));
        return toResponse(darshan);
    }

    @Transactional(readOnly = true)
    public List<DarshanResponse> listDarshans(long templeId, Authentication authentication) {
        Temple temple = requireVisibleTemple(templeId, authentication);
        boolean adminView = canManageTemple(temple.id(), authentication);
        if (adminView) {
            return darshanRepository.findByTempleId(templeId, true)
                    .stream()
                    .map(this::toResponse)
                    .toList();
        }
        List<Darshan> darshans = catalogCache.getOrLoad(
                CacheKeys.publicDarshanList(templeId),
                CatalogCacheTypeRefs.DARSHAN_LIST,
                cacheProperties.getPublicDarshanListTtl(),
                () -> darshanRepository.findByTempleId(templeId, false));
        return darshans.stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public DarshanResponse getDarshan(long templeId, long darshanId, Authentication authentication) {
        Temple temple = requireVisibleTemple(templeId, authentication);
        Darshan darshan = catalogCache.getOrLoad(
                CacheKeys.darshanId(darshanId),
                Darshan.class,
                cacheProperties.getDarshanIdTtl(),
                () -> darshanRepository.findByTempleIdAndId(templeId, darshanId)
                        .orElseThrow(() -> new ResourceNotFoundException("Darshan not found")));
        if (!canViewDarshan(darshan, temple, authentication)) {
            throw new ResourceNotFoundException("Darshan not found");
        }
        return toResponse(darshan);
    }

    @Transactional
    public DarshanResponse updateDarshan(
            long templeId,
            long darshanId,
            UpdateDarshanRequest request,
            Authentication authentication) {
        authorizationService.requireTempleManagement(authentication, templeId);
        Darshan existing = darshanRepository.findByTempleIdAndId(templeId, darshanId)
                .orElseThrow(() -> new ResourceNotFoundException("Darshan not found"));
        DarshanRepository.UpdateDarshanFields fields = new DarshanRepository.UpdateDarshanFields(
                trimToNull(request.name()),
                request.description() == null ? null : normalizeOptional(request.description()),
                request.status()
        );
        if (!darshanRepository.update(templeId, darshanId, fields)) {
            throw new ResourceNotFoundException("Darshan not found");
        }
        cacheInvalidation.invalidateAfterCommit(
                CacheKeys.darshanId(darshanId),
                CacheKeys.publicDarshanList(templeId));
        return toResponse(darshanRepository.findByTempleIdAndId(templeId, darshanId).orElse(existing));
    }

    @Transactional
    public DarshanSlotResponse createSlot(
            long templeId,
            long darshanId,
            CreateDarshanSlotRequest request,
            Authentication authentication) {
        authorizationService.requireTempleManagement(authentication, templeId);
        requireDarshanInTemple(templeId, darshanId);
        validateSlotSchedule(request.startAt(), request.endAt());
        validateCapacity(request.capacity());
        try {
            DarshanSlot slot = slotRepository.insert(
                    darshanId,
                    request.startAt(),
                    request.endAt(),
                    request.capacity(),
                    DarshanSlotStatus.AVAILABLE
            );
            return toSlotResponse(slot);
        } catch (DataIntegrityViolationException ex) {
            throw translateSlotConstraintViolation(ex);
        }
    }

    @Transactional(readOnly = true)
    public PageResponse<DarshanSlotResponse> listSlots(
            long templeId,
            long darshanId,
            Integer page,
            Integer size,
            LocalDate date,
            OffsetDateTime from,
            OffsetDateTime to,
            Authentication authentication) {
        Temple temple = requireVisibleTemple(templeId, authentication);
        Darshan darshan = darshanRepository.findByTempleIdAndId(templeId, darshanId)
                .orElseThrow(() -> new ResourceNotFoundException("Darshan not found"));
        if (!canViewDarshan(darshan, temple, authentication)) {
            throw new ResourceNotFoundException("Darshan not found");
        }
        OffsetDateTime rangeStart = null;
        OffsetDateTime rangeEnd = null;
        if (date != null) {
            SlotQuerySupport.InstantRange range = SlotQuerySupport.resolveLocalDateRange(date, temple.timezone());
            rangeStart = range.startInclusive();
            rangeEnd = range.endExclusive();
        } else if (from != null || to != null) {
            SlotQuerySupport.InstantRange range = SlotQuerySupport.resolveInstantRange(from, to);
            rangeStart = range.startInclusive();
            rangeEnd = range.endExclusive();
        }
        PageRequest pageRequest = resolve(page, size);
        boolean adminView = canManageTemple(templeId, authentication);
        List<DarshanSlotResponse> content = slotRepository
                .findByDarshanId(darshanId, adminView, rangeStart, rangeEnd, pageRequest.size(), pageRequest.offset())
                .stream()
                .map(this::toSlotResponse)
                .toList();
        long total = slotRepository.countByDarshanId(darshanId, adminView, rangeStart, rangeEnd);
        int totalPages = pageRequest.size() == 0 ? 0 : (int) Math.ceil((double) total / pageRequest.size());
        return new PageResponse<>(content, pageRequest.page(), pageRequest.size(), total, totalPages);
    }

    @Transactional(readOnly = true)
    public DarshanSlotResponse getSlot(
            long templeId,
            long darshanId,
            long slotId,
            Authentication authentication) {
        Temple temple = requireVisibleTemple(templeId, authentication);
        Darshan darshan = darshanRepository.findByTempleIdAndId(templeId, darshanId)
                .orElseThrow(() -> new ResourceNotFoundException("Darshan not found"));
        if (!canViewDarshan(darshan, temple, authentication)) {
            throw new ResourceNotFoundException("Darshan not found");
        }
        DarshanSlot slot = slotRepository.findByDarshanIdAndId(darshanId, slotId)
                .orElseThrow(() -> new ResourceNotFoundException("Darshan slot not found"));
        if (!canViewSlot(slot, templeId, authentication)) {
            throw new ResourceNotFoundException("Darshan slot not found");
        }
        return toSlotResponse(slot);
    }

    @Transactional
    public DarshanSlotResponse updateSlot(
            long templeId,
            long darshanId,
            long slotId,
            UpdateDarshanSlotRequest request,
            Authentication authentication) {
        authorizationService.requireTempleManagement(authentication, templeId);
        requireDarshanInTemple(templeId, darshanId);
        DarshanSlot existing;
        if (request.capacity() != null) {
            existing = slotRepository.lockById(slotId)
                    .filter(slot -> slot.darshanId() == darshanId)
                    .orElseThrow(() -> new ResourceNotFoundException("Darshan slot not found"));
            validateCapacity(request.capacity());
            int confirmedQuantity = bookingRepository.sumConfirmedQuantityForDarshanSlot(slotId);
            if (request.capacity() < confirmedQuantity) {
                throw new SlotCapacityBelowConfirmedBookingsException();
            }
        } else {
            existing = requireSlotInDarshan(templeId, darshanId, slotId);
        }
        OffsetDateTime startAt = request.startAt() != null ? request.startAt() : existing.startAt();
        OffsetDateTime endAt = request.endAt() != null ? request.endAt() : existing.endAt();
        validateSlotSchedule(startAt, endAt);
        if (request.status() != null) {
            validateSlotStatusTransition(existing.status(), request.status());
        }
        DarshanSlotRepository.UpdateSlotFields fields = new DarshanSlotRepository.UpdateSlotFields(
                request.startAt(),
                request.endAt(),
                request.capacity(),
                request.status()
        );
        try {
            if (!slotRepository.update(darshanId, slotId, fields)) {
                throw new ResourceNotFoundException("Darshan slot not found");
            }
        } catch (DataIntegrityViolationException ex) {
            throw translateSlotConstraintViolation(ex);
        }
        return toSlotResponse(slotRepository.findByDarshanIdAndId(darshanId, slotId).orElse(existing));
    }

    private Darshan requireDarshanInTemple(long templeId, long darshanId) {
        return darshanRepository.findByTempleIdAndId(templeId, darshanId)
                .orElseThrow(() -> new ResourceNotFoundException("Darshan not found"));
    }

    private DarshanSlot requireSlotInDarshan(long templeId, long darshanId, long slotId) {
        requireDarshanInTemple(templeId, darshanId);
        return slotRepository.findByDarshanIdAndId(darshanId, slotId)
                .orElseThrow(() -> new ResourceNotFoundException("Darshan slot not found"));
    }

    private Temple requireTempleExists(long templeId) {
        return templeRepository.findById(templeId)
                .orElseThrow(() -> new ResourceNotFoundException("Temple not found"));
    }

    private Temple requireVisibleTemple(long templeId, Authentication authentication) {
        Temple temple = templeRepository.findById(templeId)
                .orElseThrow(() -> new ResourceNotFoundException("Temple not found"));
        if (!canViewTemple(temple, authentication)) {
            throw new ResourceNotFoundException("Temple not found");
        }
        return temple;
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

    private boolean canViewDarshan(Darshan darshan, Temple temple, Authentication authentication) {
        if (canManageTemple(temple.id(), authentication)) {
            return true;
        }
        return temple.status() == TempleStatus.ACTIVE && darshan.status() == DarshanStatus.ACTIVE;
    }

    private boolean canViewSlot(DarshanSlot slot, long templeId, Authentication authentication) {
        if (canManageTemple(templeId, authentication)) {
            return true;
        }
        return slot.status() == DarshanSlotStatus.AVAILABLE && slot.endAt().isAfter(OffsetDateTime.now());
    }

    private static void validateSlotSchedule(OffsetDateTime startAt, OffsetDateTime endAt) {
        if (startAt == null || endAt == null || !endAt.isAfter(startAt)) {
            throw new InvalidSlotScheduleException();
        }
    }

    private static void validateCapacity(Integer capacity) {
        if (capacity == null || capacity <= 0) {
            throw new InvalidSlotCapacityException();
        }
    }

    private static void validateSlotStatusTransition(DarshanSlotStatus current, DarshanSlotStatus requested) {
        if (current == requested) {
            return;
        }
        boolean allowed = current == DarshanSlotStatus.AVAILABLE && requested == DarshanSlotStatus.CANCELLED;
        if (!allowed) {
            throw new InvalidSlotStatusTransitionException();
        }
    }

    private static RuntimeException translateSlotConstraintViolation(DataIntegrityViolationException ex) {
        String message = ex.getMostSpecificCause().getMessage();
        if (message != null && (message.contains("darshan_slot_no_time_overlap")
                || message.contains("conflicting key"))) {
            return new OverlappingSlotException();
        }
        if (message != null && message.contains("darshan_slot_capacity_positive")) {
            return new InvalidSlotCapacityException();
        }
        if (message != null && message.contains("darshan_slot_end_after_start")) {
            return new InvalidSlotScheduleException();
        }
        return new IllegalArgumentException("Invalid slot data");
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

    private DarshanResponse toResponse(Darshan darshan) {
        return new DarshanResponse(
                darshan.id(),
                darshan.templeId(),
                darshan.name(),
                darshan.description(),
                darshan.status(),
                darshan.createdAt(),
                darshan.updatedAt()
        );
    }

    private DarshanSlotResponse toSlotResponse(DarshanSlot slot) {
        return new DarshanSlotResponse(
                slot.id(),
                slot.darshanId(),
                slot.startAt(),
                slot.endAt(),
                slot.capacity(),
                slot.status(),
                slot.createdAt(),
                slot.updatedAt()
        );
    }
}
