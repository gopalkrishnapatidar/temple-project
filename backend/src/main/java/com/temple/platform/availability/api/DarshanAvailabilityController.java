package com.temple.platform.availability.api;

import com.temple.platform.availability.api.dto.SlotAvailabilityResponse;
import com.temple.platform.availability.service.AvailabilityService;
import com.temple.platform.temple.api.dto.PageResponse;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.OffsetDateTime;

@RestController
@RequestMapping("/api/v1/temples/{templeId}/darshans/{darshanId}")
public class DarshanAvailabilityController {

    private final AvailabilityService availabilityService;

    public DarshanAvailabilityController(AvailabilityService availabilityService) {
        this.availabilityService = availabilityService;
    }

    @GetMapping("/availability")
    public PageResponse<SlotAvailabilityResponse> listSlotAvailability(
            @PathVariable long templeId,
            @PathVariable long darshanId,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) OffsetDateTime from,
            @RequestParam(required = false) OffsetDateTime to,
            Authentication authentication) {
        return availabilityService.listDarshanSlotAvailability(
                templeId,
                darshanId,
                page,
                size,
                date,
                from,
                to,
                authentication);
    }

    @GetMapping("/slots/{slotId}/availability")
    public SlotAvailabilityResponse getSlotAvailability(
            @PathVariable long templeId,
            @PathVariable long darshanId,
            @PathVariable long slotId,
            Authentication authentication) {
        return availabilityService.getDarshanSlotAvailability(templeId, darshanId, slotId, authentication);
    }
}
