package com.temple.platform.booking.api;

import com.temple.platform.booking.api.dto.BookingResponse;
import com.temple.platform.booking.api.dto.CreateBookingRequest;
import com.temple.platform.booking.api.dto.UpdateBookingRequest;
import com.temple.platform.booking.service.BookingService;
import com.temple.platform.temple.api.dto.PageResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/bookings")
public class BookingController {

    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BookingResponse create(
            @Valid @RequestBody CreateBookingRequest request,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            Authentication authentication) {
        return bookingService.create(request, idempotencyKey, authentication);
    }

    @GetMapping
    public PageResponse<BookingResponse> list(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            Authentication authentication) {
        return bookingService.list(page, size, authentication);
    }

    @GetMapping("/{bookingReference}")
    public BookingResponse get(
            @PathVariable UUID bookingReference,
            Authentication authentication) {
        return bookingService.get(bookingReference, authentication);
    }

    @PatchMapping("/{bookingReference}")
    public BookingResponse cancel(
            @PathVariable UUID bookingReference,
            @Valid @RequestBody UpdateBookingRequest request,
            Authentication authentication) {
        return bookingService.cancel(bookingReference, request, authentication);
    }
}
