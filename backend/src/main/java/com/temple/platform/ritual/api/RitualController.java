package com.temple.platform.ritual.api;

import com.temple.platform.ritual.api.dto.CreateRitualRequest;
import com.temple.platform.ritual.api.dto.CreateRitualSlotRequest;
import com.temple.platform.ritual.api.dto.RitualResponse;
import com.temple.platform.ritual.api.dto.RitualSlotResponse;
import com.temple.platform.ritual.api.dto.UpdateRitualRequest;
import com.temple.platform.ritual.api.dto.UpdateRitualSlotRequest;
import com.temple.platform.ritual.domain.RitualType;
import com.temple.platform.ritual.service.RitualService;
import com.temple.platform.temple.api.dto.PageResponse;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/temples/{templeId}/rituals")
public class RitualController {

    private final RitualService ritualService;

    public RitualController(RitualService ritualService) {
        this.ritualService = ritualService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RitualResponse createRitual(
            @PathVariable long templeId,
            @Valid @RequestBody CreateRitualRequest request,
            Authentication authentication) {
        return ritualService.createRitual(templeId, request, authentication);
    }

    @GetMapping
    public PageResponse<RitualResponse> listRituals(
            @PathVariable long templeId,
            @RequestParam(required = false) RitualType type,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            Authentication authentication) {
        return ritualService.listRituals(templeId, type, page, size, authentication);
    }

    @GetMapping("/{ritualId}")
    public RitualResponse getRitual(
            @PathVariable long templeId,
            @PathVariable long ritualId,
            Authentication authentication) {
        return ritualService.getRitual(templeId, ritualId, authentication);
    }

    @PatchMapping("/{ritualId}")
    public RitualResponse updateRitual(
            @PathVariable long templeId,
            @PathVariable long ritualId,
            @Valid @RequestBody UpdateRitualRequest request,
            Authentication authentication) {
        return ritualService.updateRitual(templeId, ritualId, request, authentication);
    }

    @PostMapping("/{ritualId}/slots")
    @ResponseStatus(HttpStatus.CREATED)
    public RitualSlotResponse createSlot(
            @PathVariable long templeId,
            @PathVariable long ritualId,
            @Valid @RequestBody CreateRitualSlotRequest request,
            Authentication authentication) {
        return ritualService.createSlot(templeId, ritualId, request, authentication);
    }

    @GetMapping("/{ritualId}/slots")
    public PageResponse<RitualSlotResponse> listSlots(
            @PathVariable long templeId,
            @PathVariable long ritualId,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to,
            Authentication authentication) {
        return ritualService.listSlots(templeId, ritualId, page, size, date, from, to, authentication);
    }

    @GetMapping("/{ritualId}/slots/{slotId}")
    public RitualSlotResponse getSlot(
            @PathVariable long templeId,
            @PathVariable long ritualId,
            @PathVariable long slotId,
            Authentication authentication) {
        return ritualService.getSlot(templeId, ritualId, slotId, authentication);
    }

    @PatchMapping("/{ritualId}/slots/{slotId}")
    public RitualSlotResponse updateSlot(
            @PathVariable long templeId,
            @PathVariable long ritualId,
            @PathVariable long slotId,
            @Valid @RequestBody UpdateRitualSlotRequest request,
            Authentication authentication) {
        return ritualService.updateSlot(templeId, ritualId, slotId, request, authentication);
    }
}
