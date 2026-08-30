package com.temple.platform.ritual.service;

import com.temple.platform.darshan.api.SlotQuerySupport;
import com.temple.platform.ritual.api.dto.CreateRitualRequest;
import com.temple.platform.ritual.api.dto.CreateRitualSlotRequest;
import com.temple.platform.ritual.api.dto.RitualResponse;
import com.temple.platform.ritual.api.dto.RitualSlotResponse;
import com.temple.platform.ritual.api.dto.UpdateRitualRequest;
import com.temple.platform.ritual.api.dto.UpdateRitualSlotRequest;
import com.temple.platform.ritual.domain.Ritual;
import com.temple.platform.ritual.domain.RitualCurrency;
import com.temple.platform.ritual.domain.RitualSlot;
import com.temple.platform.ritual.domain.RitualSlotStatus;
import com.temple.platform.ritual.domain.RitualStatus;
import com.temple.platform.ritual.domain.RitualType;
import com.temple.platform.ritual.exception.AmbiguousSlotQueryException;
import com.temple.platform.ritual.exception.InvalidRitualCurrencyException;
import com.temple.platform.ritual.exception.InvalidRitualDurationException;
import com.temple.platform.ritual.exception.InvalidRitualNameException;
import com.temple.platform.ritual.exception.InvalidRitualPriceException;
import com.temple.platform.ritual.exception.InvalidRitualSlotScheduleException;
import com.temple.platform.ritual.exception.InvalidRitualSlotStatusTransitionException;
import com.temple.platform.ritual.repository.RitualRepository;
import com.temple.platform.ritual.repository.RitualSlotRepository;
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

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static com.temple.platform.temple.api.PaginationSupport.PageRequest;
import static com.temple.platform.temple.api.PaginationSupport.resolve;

@Service
public class RitualService {

    private final TempleRepository templeRepository;
    private final TempleAdminAssignmentRepository assignmentRepository;
    private final RitualRepository ritualRepository;
    private final RitualSlotRepository slotRepository;
    private final TempleAuthorizationService authorizationService;

    public RitualService(
            TempleRepository templeRepository,
            TempleAdminAssignmentRepository assignmentRepository,
            RitualRepository ritualRepository,
            RitualSlotRepository slotRepository,
            TempleAuthorizationService authorizationService) {
        this.templeRepository = templeRepository;
        this.assignmentRepository = assignmentRepository;
        this.ritualRepository = ritualRepository;
        this.slotRepository = slotRepository;
        this.authorizationService = authorizationService;
    }

    @Transactional
    public RitualResponse createRitual(
            long templeId,
            CreateRitualRequest request,
            Authentication authentication) {
        authorizationService.requireTempleManagement(authentication, templeId);
        requireTempleExists(templeId);
        String name = requireName(request.name());
        validateDuration(request.durationMinutes());
        validatePrice(request.price());
        RitualCurrency currency = request.currency() == null ? RitualCurrency.INR : request.currency();
        validateCurrency(currency);
        try {
            Ritual ritual = ritualRepository.insert(
                    templeId,
                    request.type(),
                    name,
                    normalizeOptional(request.description()),
                    request.durationMinutes(),
                    request.price(),
                    currency,
                    RitualStatus.ACTIVE
            );
            return toResponse(ritual);
        } catch (DataIntegrityViolationException ex) {
            throw translateRitualConstraintViolation(ex);
        }
    }

    @Transactional(readOnly = true)
    public PageResponse<RitualResponse> listRituals(
            long templeId,
            RitualType type,
            Integer page,
            Integer size,
            Authentication authentication) {
        Temple temple = requireVisibleTemple(templeId, authentication);
        boolean adminView = canManageTemple(temple.id(), authentication);
        PageRequest pageRequest = resolve(page, size);
        List<RitualResponse> content = ritualRepository
                .findByTempleId(templeId, type, adminView, pageRequest.size(), pageRequest.offset())
                .stream()
                .map(this::toResponse)
                .toList();
        long total = ritualRepository.countByTempleId(templeId, type, adminView);
        int totalPages = pageRequest.size() == 0 ? 0 : (int) Math.ceil((double) total / pageRequest.size());
        return new PageResponse<>(content, pageRequest.page(), pageRequest.size(), total, totalPages);
    }

    @Transactional(readOnly = true)
    public RitualResponse getRitual(long templeId, long ritualId, Authentication authentication) {
        Temple temple = requireVisibleTemple(templeId, authentication);
        Ritual ritual = ritualRepository.findByTempleIdAndId(templeId, ritualId)
                .orElseThrow(() -> new ResourceNotFoundException("Ritual not found"));
        if (!canViewRitual(ritual, temple, authentication)) {
            throw new ResourceNotFoundException("Ritual not found");
        }
        return toResponse(ritual);
    }

    @Transactional
    public RitualResponse updateRitual(
            long templeId,
            long ritualId,
            UpdateRitualRequest request,
            Authentication authentication) {
        authorizationService.requireTempleManagement(authentication, templeId);
        Ritual existing = ritualRepository.findByTempleIdAndId(templeId, ritualId)
                .orElseThrow(() -> new ResourceNotFoundException("Ritual not found"));
        if (request.name() != null) {
            requireName(request.name());
        }
        if (request.durationMinutes() != null) {
            validateDuration(request.durationMinutes());
        }
        if (request.price() != null) {
            validatePrice(request.price());
        }
        if (request.currency() != null) {
            validateCurrency(request.currency());
        }
        RitualRepository.UpdateRitualFields fields = new RitualRepository.UpdateRitualFields(
                request.type(),
                request.name() == null ? null : requireName(request.name()),
                request.description() == null ? null : normalizeOptional(request.description()),
                request.durationMinutes(),
                request.price(),
                request.currency(),
                request.status()
        );
        try {
            if (!ritualRepository.update(templeId, ritualId, fields)) {
                throw new ResourceNotFoundException("Ritual not found");
            }
        } catch (DataIntegrityViolationException ex) {
            throw translateRitualConstraintViolation(ex);
        }
        return toResponse(ritualRepository.findByTempleIdAndId(templeId, ritualId).orElse(existing));
    }

    @Transactional
    public RitualSlotResponse createSlot(
            long templeId,
            long ritualId,
            CreateRitualSlotRequest request,
            Authentication authentication) {
        authorizationService.requireTempleManagement(authentication, templeId);
        requireRitualInTemple(templeId, ritualId);
        validateSlotSchedule(request.startAt(), request.endAt());
        try {
            RitualSlot slot = slotRepository.insert(
                    ritualId,
                    request.startAt(),
                    request.endAt(),
                    RitualSlotStatus.AVAILABLE
            );
            return toSlotResponse(slot);
        } catch (DataIntegrityViolationException ex) {
            throw translateSlotConstraintViolation(ex);
        }
    }

    @Transactional(readOnly = true)
    public PageResponse<RitualSlotResponse> listSlots(
            long templeId,
            long ritualId,
            Integer page,
            Integer size,
            LocalDate date,
            Instant from,
            Instant to,
            Authentication authentication) {
        Temple temple = requireVisibleTemple(templeId, authentication);
        Ritual ritual = ritualRepository.findByTempleIdAndId(templeId, ritualId)
                .orElseThrow(() -> new ResourceNotFoundException("Ritual not found"));
        if (!canViewRitual(ritual, temple, authentication)) {
            throw new ResourceNotFoundException("Ritual not found");
        }
        if (date != null && (from != null || to != null)) {
            throw new AmbiguousSlotQueryException();
        }
        Instant rangeStart = null;
        Instant rangeEnd = null;
        if (date != null) {
            SlotQuerySupport.InstantRange range = SlotQuerySupport.resolveLocalDateRange(date, temple.timezone());
            rangeStart = range.startInclusive().toInstant();
            rangeEnd = range.endExclusive().toInstant();
        } else if (from != null || to != null) {
            SlotQuerySupport.InstantRange range = SlotQuerySupport.resolveInstantRange(
                    toOffsetDateTime(from),
                    toOffsetDateTime(to)
            );
            rangeStart = range.startInclusive().toInstant();
            rangeEnd = range.endExclusive().toInstant();
        }
        PageRequest pageRequest = resolve(page, size);
        boolean adminView = canManageTemple(templeId, authentication);
        List<RitualSlotResponse> content = slotRepository
                .findByRitualId(ritualId, adminView, rangeStart, rangeEnd, pageRequest.size(), pageRequest.offset())
                .stream()
                .map(this::toSlotResponse)
                .toList();
        long total = slotRepository.countByRitualId(ritualId, adminView, rangeStart, rangeEnd);
        int totalPages = pageRequest.size() == 0 ? 0 : (int) Math.ceil((double) total / pageRequest.size());
        return new PageResponse<>(content, pageRequest.page(), pageRequest.size(), total, totalPages);
    }

    @Transactional(readOnly = true)
    public RitualSlotResponse getSlot(
            long templeId,
            long ritualId,
            long slotId,
            Authentication authentication) {
        Temple temple = requireVisibleTemple(templeId, authentication);
        Ritual ritual = ritualRepository.findByTempleIdAndId(templeId, ritualId)
                .orElseThrow(() -> new ResourceNotFoundException("Ritual not found"));
        if (!canViewRitual(ritual, temple, authentication)) {
            throw new ResourceNotFoundException("Ritual not found");
        }
        RitualSlot slot = slotRepository.findByRitualIdAndId(ritualId, slotId)
                .orElseThrow(() -> new ResourceNotFoundException("Ritual slot not found"));
        if (!canViewSlot(slot, templeId, authentication)) {
            throw new ResourceNotFoundException("Ritual slot not found");
        }
        return toSlotResponse(slot);
    }

    @Transactional
    public RitualSlotResponse updateSlot(
            long templeId,
            long ritualId,
            long slotId,
            UpdateRitualSlotRequest request,
            Authentication authentication) {
        authorizationService.requireTempleManagement(authentication, templeId);
        RitualSlot existing = requireSlotInRitual(templeId, ritualId, slotId);
        Instant startAt = request.startAt() != null ? request.startAt() : existing.startAt();
        Instant endAt = request.endAt() != null ? request.endAt() : existing.endAt();
        validateSlotSchedule(startAt, endAt);
        if (request.status() != null) {
            validateSlotStatusTransition(existing.status(), request.status());
        }
        RitualSlotRepository.UpdateSlotFields fields = new RitualSlotRepository.UpdateSlotFields(
                request.startAt(),
                request.endAt(),
                request.status()
        );
        try {
            if (!slotRepository.update(ritualId, slotId, fields)) {
                throw new ResourceNotFoundException("Ritual slot not found");
            }
        } catch (DataIntegrityViolationException ex) {
            throw translateSlotConstraintViolation(ex);
        }
        return toSlotResponse(slotRepository.findByRitualIdAndId(ritualId, slotId).orElse(existing));
    }

    private Ritual requireRitualInTemple(long templeId, long ritualId) {
        return ritualRepository.findByTempleIdAndId(templeId, ritualId)
                .orElseThrow(() -> new ResourceNotFoundException("Ritual not found"));
    }

    private RitualSlot requireSlotInRitual(long templeId, long ritualId, long slotId) {
        requireRitualInTemple(templeId, ritualId);
        return slotRepository.findByRitualIdAndId(ritualId, slotId)
                .orElseThrow(() -> new ResourceNotFoundException("Ritual slot not found"));
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

    private boolean canViewRitual(Ritual ritual, Temple temple, Authentication authentication) {
        if (canManageTemple(temple.id(), authentication)) {
            return true;
        }
        return temple.status() == TempleStatus.ACTIVE && ritual.status() == RitualStatus.ACTIVE;
    }

    private boolean canViewSlot(RitualSlot slot, long templeId, Authentication authentication) {
        if (canManageTemple(templeId, authentication)) {
            return true;
        }
        return slot.status() == RitualSlotStatus.AVAILABLE && slot.endAt().isAfter(Instant.now());
    }

    private static String requireName(String name) {
        if (name == null || name.isBlank()) {
            throw new InvalidRitualNameException();
        }
        return name.trim();
    }

    private static void validateDuration(Integer durationMinutes) {
        if (durationMinutes == null || durationMinutes <= 0) {
            throw new InvalidRitualDurationException();
        }
    }

    private static void validatePrice(BigDecimal price) {
        if (price == null || price.compareTo(BigDecimal.ZERO) < 0) {
            throw new InvalidRitualPriceException();
        }
    }

    private static void validateCurrency(RitualCurrency currency) {
        if (currency == null || currency != RitualCurrency.INR) {
            throw new InvalidRitualCurrencyException();
        }
    }

    private static void validateSlotSchedule(Instant startAt, Instant endAt) {
        if (startAt == null || endAt == null || !endAt.isAfter(startAt)) {
            throw new InvalidRitualSlotScheduleException();
        }
    }

    private static void validateSlotStatusTransition(RitualSlotStatus current, RitualSlotStatus requested) {
        if (current == requested) {
            return;
        }
        boolean allowed = current == RitualSlotStatus.AVAILABLE && requested == RitualSlotStatus.CANCELLED;
        if (!allowed) {
            throw new InvalidRitualSlotStatusTransitionException();
        }
    }

    private static RuntimeException translateRitualConstraintViolation(DataIntegrityViolationException ex) {
        String message = ex.getMostSpecificCause().getMessage();
        if (message != null && message.contains("ritual_name_not_blank")) {
            return new InvalidRitualNameException();
        }
        if (message != null && message.contains("ritual_duration_positive")) {
            return new InvalidRitualDurationException();
        }
        if (message != null && message.contains("ritual_price_non_negative")) {
            return new InvalidRitualPriceException();
        }
        if (message != null && message.contains("ritual_currency_supported")) {
            return new InvalidRitualCurrencyException();
        }
        return new IllegalArgumentException("Invalid ritual data");
    }

    private static RuntimeException translateSlotConstraintViolation(DataIntegrityViolationException ex) {
        String message = ex.getMostSpecificCause().getMessage();
        if (message != null && message.contains("ritual_slot_end_after_start")) {
            return new InvalidRitualSlotScheduleException();
        }
        return new IllegalArgumentException("Invalid slot data");
    }

    private static OffsetDateTime toOffsetDateTime(Instant instant) {
        if (instant == null) {
            return null;
        }
        return instant.atOffset(ZoneOffset.UTC);
    }

    private static String normalizeOptional(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private RitualResponse toResponse(Ritual ritual) {
        return new RitualResponse(
                ritual.id(),
                ritual.templeId(),
                ritual.type(),
                ritual.name(),
                ritual.description(),
                ritual.durationMinutes(),
                ritual.price(),
                ritual.currency(),
                ritual.status(),
                ritual.createdAt(),
                ritual.updatedAt()
        );
    }

    private RitualSlotResponse toSlotResponse(RitualSlot slot) {
        return new RitualSlotResponse(
                slot.id(),
                slot.ritualId(),
                slot.startAt(),
                slot.endAt(),
                slot.status(),
                slot.createdAt(),
                slot.updatedAt()
        );
    }
}
