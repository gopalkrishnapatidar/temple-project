package com.temple.platform.availability.service;

import com.temple.platform.availability.api.dto.SlotAvailabilityResponse;
import com.temple.platform.availability.repository.AvailabilityRepository;
import com.temple.platform.availability.repository.AvailabilityRepository.DarshanSlotAvailabilityRow;
import com.temple.platform.availability.repository.AvailabilityRepository.RitualSlotAvailabilityRow;
import com.temple.platform.darshan.api.SlotQuerySupport;
import com.temple.platform.darshan.domain.Darshan;
import com.temple.platform.darshan.domain.DarshanSlotStatus;
import com.temple.platform.darshan.domain.DarshanStatus;
import com.temple.platform.darshan.repository.DarshanRepository;
import com.temple.platform.ritual.domain.Ritual;
import com.temple.platform.ritual.domain.RitualSlotStatus;
import com.temple.platform.ritual.domain.RitualStatus;
import com.temple.platform.ritual.exception.AmbiguousSlotQueryException;
import com.temple.platform.ritual.repository.RitualRepository;
import com.temple.platform.temple.api.dto.PageResponse;
import com.temple.platform.temple.domain.Temple;
import com.temple.platform.temple.domain.TempleStatus;
import com.temple.platform.temple.exception.ResourceNotFoundException;
import com.temple.platform.temple.repository.TempleAdminAssignmentRepository;
import com.temple.platform.temple.repository.TempleRepository;
import com.temple.platform.temple.security.TempleAuthorizationService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static com.temple.platform.temple.api.PaginationSupport.PageRequest;
import static com.temple.platform.temple.api.PaginationSupport.resolve;

@Service
public class AvailabilityService {

    private final TempleRepository templeRepository;
    private final TempleAdminAssignmentRepository assignmentRepository;
    private final DarshanRepository darshanRepository;
    private final RitualRepository ritualRepository;
    private final AvailabilityRepository availabilityRepository;
    private final TempleAuthorizationService authorizationService;
    private final Clock clock;

    public AvailabilityService(
            TempleRepository templeRepository,
            TempleAdminAssignmentRepository assignmentRepository,
            DarshanRepository darshanRepository,
            RitualRepository ritualRepository,
            AvailabilityRepository availabilityRepository,
            TempleAuthorizationService authorizationService,
            Clock clock) {
        this.templeRepository = templeRepository;
        this.assignmentRepository = assignmentRepository;
        this.darshanRepository = darshanRepository;
        this.ritualRepository = ritualRepository;
        this.availabilityRepository = availabilityRepository;
        this.authorizationService = authorizationService;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public PageResponse<SlotAvailabilityResponse> listDarshanSlotAvailability(
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
        Instant now = clock.instant();
        List<SlotAvailabilityResponse> content = availabilityRepository
                .findDarshanSlotAvailabilities(
                        darshanId,
                        adminView,
                        rangeStart,
                        rangeEnd,
                        pageRequest.size(),
                        pageRequest.offset())
                .stream()
                .map(row -> toDarshanResponse(row, now))
                .toList();
        long total = availabilityRepository.countDarshanSlotAvailabilities(
                darshanId,
                adminView,
                rangeStart,
                rangeEnd);
        int totalPages = pageRequest.size() == 0 ? 0 : (int) Math.ceil((double) total / pageRequest.size());
        return new PageResponse<>(content, pageRequest.page(), pageRequest.size(), total, totalPages);
    }

    @Transactional(readOnly = true)
    public SlotAvailabilityResponse getDarshanSlotAvailability(
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
        DarshanSlotAvailabilityRow row = availabilityRepository.findDarshanSlotAvailability(darshanId, slotId)
                .orElseThrow(() -> new ResourceNotFoundException("Darshan slot not found"));
        if (!canViewDarshanSlot(row, templeId, authentication)) {
            throw new ResourceNotFoundException("Darshan slot not found");
        }
        return toDarshanResponse(row, clock.instant());
    }

    @Transactional(readOnly = true)
    public PageResponse<SlotAvailabilityResponse> listRitualSlotAvailability(
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
                    toOffsetDateTime(to));
            rangeStart = range.startInclusive().toInstant();
            rangeEnd = range.endExclusive().toInstant();
        }
        PageRequest pageRequest = resolve(page, size);
        boolean adminView = canManageTemple(templeId, authentication);
        Instant now = clock.instant();
        List<SlotAvailabilityResponse> content = availabilityRepository
                .findRitualSlotAvailabilities(
                        ritualId,
                        adminView,
                        rangeStart,
                        rangeEnd,
                        pageRequest.size(),
                        pageRequest.offset())
                .stream()
                .map(row -> toRitualResponse(row, now))
                .toList();
        long total = availabilityRepository.countRitualSlotAvailabilities(
                ritualId,
                adminView,
                rangeStart,
                rangeEnd);
        int totalPages = pageRequest.size() == 0 ? 0 : (int) Math.ceil((double) total / pageRequest.size());
        return new PageResponse<>(content, pageRequest.page(), pageRequest.size(), total, totalPages);
    }

    @Transactional(readOnly = true)
    public SlotAvailabilityResponse getRitualSlotAvailability(
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
        RitualSlotAvailabilityRow row = availabilityRepository.findRitualSlotAvailability(ritualId, slotId)
                .orElseThrow(() -> new ResourceNotFoundException("Ritual slot not found"));
        if (!canViewRitualSlot(row, templeId, authentication)) {
            throw new ResourceNotFoundException("Ritual slot not found");
        }
        return toRitualResponse(row, clock.instant());
    }

    static SlotAvailabilityResponse toDarshanResponse(DarshanSlotAvailabilityRow row, Instant now) {
        int bookedQuantity = row.bookedQuantity();
        int remainingCapacity = Math.max(0, row.capacity() - bookedQuantity);
        boolean available = isDarshanSlotBookable(row.status(), row.endAt(), now) && remainingCapacity > 0;
        return new SlotAvailabilityResponse(
                row.slotId(),
                row.capacity(),
                bookedQuantity,
                remainingCapacity,
                available
        );
    }

    static SlotAvailabilityResponse toRitualResponse(RitualSlotAvailabilityRow row, Instant now) {
        int bookedQuantity = row.bookedQuantity();
        int remainingCapacity = Math.max(0, row.capacity() - bookedQuantity);
        boolean available = isRitualSlotBookable(row.status(), row.endAt(), now) && remainingCapacity > 0;
        return new SlotAvailabilityResponse(
                row.slotId(),
                row.capacity(),
                bookedQuantity,
                remainingCapacity,
                available
        );
    }

    private static boolean isDarshanSlotBookable(DarshanSlotStatus status, OffsetDateTime endAt, Instant now) {
        return status == DarshanSlotStatus.AVAILABLE && endAt.toInstant().isAfter(now);
    }

    private static boolean isRitualSlotBookable(RitualSlotStatus status, Instant endAt, Instant now) {
        return status == RitualSlotStatus.AVAILABLE && endAt.isAfter(now);
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

    private boolean canViewRitual(Ritual ritual, Temple temple, Authentication authentication) {
        if (canManageTemple(temple.id(), authentication)) {
            return true;
        }
        return temple.status() == TempleStatus.ACTIVE && ritual.status() == RitualStatus.ACTIVE;
    }

    private boolean canViewDarshanSlot(
            DarshanSlotAvailabilityRow row,
            long templeId,
            Authentication authentication) {
        if (canManageTemple(templeId, authentication)) {
            return true;
        }
        return row.status() == DarshanSlotStatus.AVAILABLE && row.endAt().isAfter(OffsetDateTime.now());
    }

    private boolean canViewRitualSlot(
            RitualSlotAvailabilityRow row,
            long templeId,
            Authentication authentication) {
        if (canManageTemple(templeId, authentication)) {
            return true;
        }
        return row.status() == RitualSlotStatus.AVAILABLE && row.endAt().isAfter(clock.instant());
    }

    private static OffsetDateTime toOffsetDateTime(Instant instant) {
        if (instant == null) {
            return null;
        }
        return instant.atOffset(ZoneOffset.UTC);
    }
}
