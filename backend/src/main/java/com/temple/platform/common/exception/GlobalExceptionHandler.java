package com.temple.platform.common.exception;

import com.temple.platform.darshan.exception.InvalidSlotCapacityException;
import com.temple.platform.darshan.exception.InvalidSlotScheduleException;
import com.temple.platform.darshan.exception.InvalidSlotStatusTransitionException;
import com.temple.platform.darshan.exception.OverlappingSlotException;
import com.temple.platform.identity.exception.DuplicateEmailException;
import com.temple.platform.ritual.exception.AmbiguousSlotQueryException;
import com.temple.platform.ritual.exception.InvalidRitualCurrencyException;
import com.temple.platform.ritual.exception.InvalidRitualDurationException;
import com.temple.platform.ritual.exception.InvalidRitualNameException;
import com.temple.platform.ritual.exception.InvalidRitualPriceException;
import com.temple.platform.ritual.exception.InvalidRitualSlotScheduleException;
import com.temple.platform.ritual.exception.InvalidRitualSlotStatusTransitionException;
import com.temple.platform.temple.exception.DuplicateAssignmentException;
import com.temple.platform.temple.exception.ForbiddenOperationException;
import com.temple.platform.temple.exception.InvalidEventScheduleException;
import com.temple.platform.temple.exception.InvalidEventStatusTransitionException;
import com.temple.platform.temple.exception.ResourceNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.Instant;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(
            NoResourceFoundException ex,
            HttpServletRequest request) {
        return buildResponse(HttpStatus.NOT_FOUND, ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(
            MethodArgumentNotValidException ex,
            HttpServletRequest request) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + " " + error.getDefaultMessage())
                .collect(Collectors.joining("; "));
        if (message.isBlank()) {
            message = "Invalid request";
        }
        return buildResponse(HttpStatus.BAD_REQUEST, message, request.getRequestURI());
    }

    @ExceptionHandler({
            HttpMessageNotReadableException.class,
            IllegalArgumentException.class,
            MethodArgumentTypeMismatchException.class
    })
    public ResponseEntity<ErrorResponse> handleBadRequest(
            Exception ex,
            HttpServletRequest request) {
        return buildResponse(HttpStatus.BAD_REQUEST, "Invalid request", request.getRequestURI());
    }

    @ExceptionHandler(InvalidEventScheduleException.class)
    public ResponseEntity<ErrorResponse> handleInvalidEventSchedule(
            InvalidEventScheduleException ex,
            HttpServletRequest request) {
        return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(InvalidEventStatusTransitionException.class)
    public ResponseEntity<ErrorResponse> handleInvalidEventStatusTransition(
            InvalidEventStatusTransitionException ex,
            HttpServletRequest request) {
        return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(DuplicateAssignmentException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateAssignment(
            DuplicateAssignmentException ex,
            HttpServletRequest request) {
        return buildResponse(HttpStatus.CONFLICT, ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFound(
            ResourceNotFoundException ex,
            HttpServletRequest request) {
        return buildResponse(HttpStatus.NOT_FOUND, ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(ForbiddenOperationException.class)
    public ResponseEntity<ErrorResponse> handleForbiddenOperation(
            ForbiddenOperationException ex,
            HttpServletRequest request) {
        return buildResponse(HttpStatus.FORBIDDEN, ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(InvalidSlotScheduleException.class)
    public ResponseEntity<ErrorResponse> handleInvalidSlotSchedule(
            InvalidSlotScheduleException ex,
            HttpServletRequest request) {
        return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(InvalidSlotCapacityException.class)
    public ResponseEntity<ErrorResponse> handleInvalidSlotCapacity(
            InvalidSlotCapacityException ex,
            HttpServletRequest request) {
        return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(InvalidSlotStatusTransitionException.class)
    public ResponseEntity<ErrorResponse> handleInvalidSlotStatusTransition(
            InvalidSlotStatusTransitionException ex,
            HttpServletRequest request) {
        return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(OverlappingSlotException.class)
    public ResponseEntity<ErrorResponse> handleOverlappingSlot(
            OverlappingSlotException ex,
            HttpServletRequest request) {
        return buildResponse(HttpStatus.CONFLICT, ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler({
            InvalidRitualNameException.class,
            InvalidRitualDurationException.class,
            InvalidRitualPriceException.class,
            InvalidRitualCurrencyException.class,
            InvalidRitualSlotScheduleException.class,
            InvalidRitualSlotStatusTransitionException.class,
            AmbiguousSlotQueryException.class
    })
    public ResponseEntity<ErrorResponse> handleInvalidRitual(
            RuntimeException ex,
            HttpServletRequest request) {
        return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(DuplicateEmailException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateEmail(
            DuplicateEmailException ex,
            HttpServletRequest request) {
        return buildResponse(HttpStatus.CONFLICT, ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthentication(
            AuthenticationException ex,
            HttpServletRequest request) {
        return buildResponse(HttpStatus.UNAUTHORIZED, "Invalid credentials", request.getRequestURI());
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(
            AccessDeniedException ex,
            HttpServletRequest request) {
        return buildResponse(HttpStatus.FORBIDDEN, "Access denied", request.getRequestURI());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(
            Exception ex,
            HttpServletRequest request) {
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred", request.getRequestURI());
    }

    private ResponseEntity<ErrorResponse> buildResponse(
            HttpStatus status,
            String message,
            String path) {
        ErrorResponse body = new ErrorResponse(
                Instant.now(),
                status.value(),
                status.getReasonPhrase(),
                message,
                path
        );
        return ResponseEntity.status(status).body(body);
    }
}
