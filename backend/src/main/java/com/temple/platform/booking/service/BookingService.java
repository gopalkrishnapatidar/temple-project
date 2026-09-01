package com.temple.platform.booking.service;

import com.temple.platform.booking.api.dto.BookingResponse;
import com.temple.platform.booking.api.dto.CreateBookingRequest;
import com.temple.platform.booking.api.dto.UpdateBookingRequest;
import com.temple.platform.booking.domain.Booking;
import com.temple.platform.booking.domain.BookingStatus;
import com.temple.platform.booking.domain.BookingTargetType;
import com.temple.platform.booking.exception.BookingConflictException;
import com.temple.platform.booking.exception.IdempotencyConflictException;
import com.temple.platform.booking.exception.InsufficientCapacityException;
import com.temple.platform.booking.exception.InvalidBookingUpdateException;
import com.temple.platform.booking.exception.InvalidIdempotencyKeyException;
import com.temple.platform.booking.repository.BookingRepository;
import com.temple.platform.darshan.domain.Darshan;
import com.temple.platform.darshan.domain.DarshanSlot;
import com.temple.platform.darshan.domain.DarshanSlotStatus;
import com.temple.platform.darshan.domain.DarshanStatus;
import com.temple.platform.darshan.repository.DarshanRepository;
import com.temple.platform.darshan.repository.DarshanSlotRepository;
import com.temple.platform.ritual.domain.Ritual;
import com.temple.platform.ritual.domain.RitualSlot;
import com.temple.platform.ritual.domain.RitualSlotStatus;
import com.temple.platform.ritual.domain.RitualStatus;
import com.temple.platform.ritual.repository.RitualRepository;
import com.temple.platform.ritual.repository.RitualSlotRepository;
import com.temple.platform.temple.api.dto.PageResponse;
import com.temple.platform.temple.domain.Temple;
import com.temple.platform.temple.domain.TempleStatus;
import com.temple.platform.temple.exception.ForbiddenOperationException;
import com.temple.platform.temple.exception.ResourceNotFoundException;
import com.temple.platform.temple.repository.TempleAdminAssignmentRepository;
import com.temple.platform.temple.repository.TempleRepository;
import com.temple.platform.temple.security.TempleAuthorizationService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.temple.platform.temple.api.PaginationSupport.PageRequest;
import static com.temple.platform.temple.api.PaginationSupport.resolve;

@Service
public class BookingService {

    static final int MAX_QUANTITY = 50;
    static final int MAX_IDEMPOTENCY_KEY_LENGTH = 128;

    private final BookingRepository bookingRepository;
    private final DarshanSlotRepository darshanSlotRepository;
    private final DarshanRepository darshanRepository;
    private final RitualSlotRepository ritualSlotRepository;
    private final RitualRepository ritualRepository;
    private final TempleRepository templeRepository;
    private final TempleAdminAssignmentRepository assignmentRepository;
    private final TempleAuthorizationService authorizationService;
    private final Clock clock;

    public BookingService(
            BookingRepository bookingRepository,
            DarshanSlotRepository darshanSlotRepository,
            DarshanRepository darshanRepository,
            RitualSlotRepository ritualSlotRepository,
            RitualRepository ritualRepository,
            TempleRepository templeRepository,
            TempleAdminAssignmentRepository assignmentRepository,
            TempleAuthorizationService authorizationService,
            Clock clock) {
        this.bookingRepository = bookingRepository;
        this.darshanSlotRepository = darshanSlotRepository;
        this.darshanRepository = darshanRepository;
        this.ritualSlotRepository = ritualSlotRepository;
        this.ritualRepository = ritualRepository;
        this.templeRepository = templeRepository;
        this.assignmentRepository = assignmentRepository;
        this.authorizationService = authorizationService;
        this.clock = clock;
    }

    @Transactional
    public BookingResponse create(
            CreateBookingRequest request,
            String idempotencyKey,
            Authentication authentication) {
        if (!authorizationService.isDevotee(authentication)) {
            throw new ForbiddenOperationException("Only devotees can create bookings");
        }
        long accountId = authorizationService.requireAccountId(authentication);
        String key = requireIdempotencyKey(idempotencyKey);
        int quantity = requireQuantity(request.quantity());
        Instant now = clock.instant();
        if (request.targetType() == BookingTargetType.DARSHAN) {
            return createDarshanBooking(accountId, key, request.slotId(), quantity, now);
        }
        return createRitualBooking(accountId, key, request.slotId(), quantity, now);
    }

    @Transactional(readOnly = true)
    public PageResponse<BookingResponse> list(Integer page, Integer size, Authentication authentication) {
        long accountId = authorizationService.requireAccountId(authentication);
        PageRequest pageRequest = resolve(page, size);
        List<Booking> bookings;
        long total;
        if (authorizationService.isPlatformAdmin(authentication)) {
            bookings = bookingRepository.findAll(pageRequest.size(), pageRequest.offset());
            total = bookingRepository.countAll();
        } else if (authorizationService.isTempleAdmin(authentication)) {
            bookings = bookingRepository.findForTempleAdmin(accountId, pageRequest.size(), pageRequest.offset());
            total = bookingRepository.countForTempleAdmin(accountId);
        } else {
            bookings = bookingRepository.findByAccountId(accountId, pageRequest.size(), pageRequest.offset());
            total = bookingRepository.countByAccountId(accountId);
        }
        List<BookingResponse> content = bookings.stream().map(BookingService::toResponse).toList();
        int totalPages = pageRequest.size() == 0 ? 0 : (int) Math.ceil((double) total / pageRequest.size());
        return new PageResponse<>(content, pageRequest.page(), pageRequest.size(), total, totalPages);
    }

    @Transactional(readOnly = true)
    public BookingResponse get(UUID bookingReference, Authentication authentication) {
        Booking booking = bookingRepository.findByBookingReference(bookingReference)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));
        if (!canAccess(booking, authentication)) {
            throw new ResourceNotFoundException("Booking not found");
        }
        return toResponse(booking);
    }

    @Transactional
    public BookingResponse cancel(UUID bookingReference, UpdateBookingRequest request, Authentication authentication) {
        if (request.status() != BookingStatus.CANCELLED) {
            throw new InvalidBookingUpdateException();
        }
        Booking booking = bookingRepository.findByBookingReference(bookingReference)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));
        if (!canAccess(booking, authentication)) {
            throw new ResourceNotFoundException("Booking not found");
        }
        if (booking.status() == BookingStatus.CANCELLED) {
            return toResponse(booking);
        }
        lockSlot(booking);
        Booking locked = bookingRepository.findById(booking.id())
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));
        if (locked.status() == BookingStatus.CANCELLED) {
            return toResponse(locked);
        }
        bookingRepository.updateStatus(locked.id(), BookingStatus.CANCELLED);
        return toResponse(bookingRepository.findById(locked.id()).orElse(locked));
    }

    private BookingResponse createDarshanBooking(
            long accountId,
            String idempotencyKey,
            long slotId,
            int quantity,
            Instant now) {
        DarshanSlot slot = darshanSlotRepository.lockById(slotId)
                .orElseThrow(() -> new ResourceNotFoundException("Slot not found"));
        requireBookableDarshanSlot(slot, now);
        Optional<Booking> existing = bookingRepository.findByAccountIdAndIdempotencyKey(accountId, idempotencyKey);
        if (existing.isPresent()) {
            return replayOrConflict(existing.get(), BookingTargetType.DARSHAN, slotId, quantity);
        }
        int reserved = bookingRepository.sumConfirmedQuantityForDarshanSlot(slot.id());
        if (quantity > slot.capacity() - reserved) {
            throw new InsufficientCapacityException();
        }
        Optional<Booking> inserted = bookingRepository.insertIgnoringIdempotencyConflict(
                UUID.randomUUID(),
                accountId,
                slot.id(),
                null,
                quantity,
                BookingStatus.CONFIRMED,
                idempotencyKey
        );
        if (inserted.isEmpty()) {
            Booking concurrent = bookingRepository.findByAccountIdAndIdempotencyKey(accountId, idempotencyKey)
                    .orElseThrow(() -> new IllegalStateException("Idempotent booking not found after conflict"));
            return replayOrConflict(concurrent, BookingTargetType.DARSHAN, slotId, quantity);
        }
        return toResponse(inserted.get());
    }

    private BookingResponse createRitualBooking(
            long accountId,
            String idempotencyKey,
            long slotId,
            int quantity,
            Instant now) {
        RitualSlot slot = ritualSlotRepository.lockById(slotId)
                .orElseThrow(() -> new ResourceNotFoundException("Slot not found"));
        requireBookableRitualSlot(slot, now);
        Optional<Booking> existing = bookingRepository.findByAccountIdAndIdempotencyKey(accountId, idempotencyKey);
        if (existing.isPresent()) {
            return replayOrConflict(existing.get(), BookingTargetType.RITUAL, slotId, quantity);
        }
        int reserved = bookingRepository.sumConfirmedQuantityForRitualSlot(slot.id());
        if (quantity > slot.capacity() - reserved) {
            throw new InsufficientCapacityException();
        }
        Optional<Booking> inserted = bookingRepository.insertIgnoringIdempotencyConflict(
                UUID.randomUUID(),
                accountId,
                null,
                slot.id(),
                quantity,
                BookingStatus.CONFIRMED,
                idempotencyKey
        );
        if (inserted.isEmpty()) {
            Booking concurrent = bookingRepository.findByAccountIdAndIdempotencyKey(accountId, idempotencyKey)
                    .orElseThrow(() -> new IllegalStateException("Idempotent booking not found after conflict"));
            return replayOrConflict(concurrent, BookingTargetType.RITUAL, slotId, quantity);
        }
        return toResponse(inserted.get());
    }

    private void requireBookableDarshanSlot(DarshanSlot slot, Instant now) {
        if (slot.status() != DarshanSlotStatus.AVAILABLE || !slot.endAt().toInstant().isAfter(now)) {
            throw new BookingConflictException();
        }
        Darshan darshan = darshanRepository.findById(slot.darshanId())
                .orElseThrow(() -> new ResourceNotFoundException("Slot not found"));
        Temple temple = templeRepository.findById(darshan.templeId())
                .orElseThrow(() -> new ResourceNotFoundException("Slot not found"));
        if (temple.status() != TempleStatus.ACTIVE || darshan.status() != DarshanStatus.ACTIVE) {
            throw new ResourceNotFoundException("Slot not found");
        }
    }

    private void requireBookableRitualSlot(RitualSlot slot, Instant now) {
        if (slot.status() != RitualSlotStatus.AVAILABLE || !slot.endAt().isAfter(now)) {
            throw new BookingConflictException();
        }
        Ritual ritual = ritualRepository.findById(slot.ritualId())
                .orElseThrow(() -> new ResourceNotFoundException("Slot not found"));
        Temple temple = templeRepository.findById(ritual.templeId())
                .orElseThrow(() -> new ResourceNotFoundException("Slot not found"));
        if (temple.status() != TempleStatus.ACTIVE || ritual.status() != RitualStatus.ACTIVE) {
            throw new ResourceNotFoundException("Slot not found");
        }
    }

    private void lockSlot(Booking booking) {
        if (booking.darshanSlotId() != null) {
            darshanSlotRepository.lockById(booking.darshanSlotId())
                    .orElseThrow(() -> new ResourceNotFoundException("Slot not found"));
            return;
        }
        ritualSlotRepository.lockById(booking.ritualSlotId())
                .orElseThrow(() -> new ResourceNotFoundException("Slot not found"));
    }

    private boolean canAccess(Booking booking, Authentication authentication) {
        long accountId = authorizationService.requireAccountId(authentication);
        if (booking.accountId() == accountId) {
            return true;
        }
        if (authorizationService.isPlatformAdmin(authentication)) {
            return true;
        }
        if (authorizationService.isTempleAdmin(authentication)) {
            Long templeId = bookingRepository.findTempleIdByBookingId(booking.id()).orElse(null);
            return templeId != null && assignmentRepository.exists(accountId, templeId);
        }
        return false;
    }

    private static BookingResponse replayOrConflict(
            Booking existing,
            BookingTargetType targetType,
            long slotId,
            int quantity) {
        boolean sameRequest = existing.targetType() == targetType
                && existing.slotId() == slotId
                && existing.quantity() == quantity;
        if (sameRequest) {
            return toResponse(existing);
        }
        throw new IdempotencyConflictException();
    }

    static String requireIdempotencyKey(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new InvalidIdempotencyKeyException();
        }
        String trimmed = idempotencyKey.trim();
        if (trimmed.length() > MAX_IDEMPOTENCY_KEY_LENGTH) {
            throw new InvalidIdempotencyKeyException();
        }
        return trimmed;
    }

    static int requireQuantity(Integer quantity) {
        if (quantity == null || quantity < 1 || quantity > MAX_QUANTITY) {
            throw new IllegalArgumentException("Invalid quantity");
        }
        return quantity;
    }

    private static BookingResponse toResponse(Booking booking) {
        return new BookingResponse(
                booking.bookingReference(),
                booking.targetType(),
                booking.slotId(),
                booking.quantity(),
                booking.status(),
                booking.createdAt(),
                booking.updatedAt()
        );
    }
}
