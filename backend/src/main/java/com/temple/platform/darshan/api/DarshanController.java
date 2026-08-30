package com.temple.platform.darshan.api;

import com.temple.platform.darshan.api.dto.CreateDarshanRequest;
import com.temple.platform.darshan.api.dto.CreateDarshanSlotRequest;
import com.temple.platform.darshan.api.dto.DarshanResponse;
import com.temple.platform.darshan.api.dto.DarshanSlotResponse;
import com.temple.platform.darshan.api.dto.UpdateDarshanRequest;
import com.temple.platform.darshan.api.dto.UpdateDarshanSlotRequest;
import com.temple.platform.darshan.service.DarshanService;
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

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/temples/{templeId}/darshans")
public class DarshanController {

    private final DarshanService darshanService;

    public DarshanController(DarshanService darshanService) {
        this.darshanService = darshanService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DarshanResponse createDarshan(
            @PathVariable long templeId,
            @Valid @RequestBody CreateDarshanRequest request,
            Authentication authentication) {
        return darshanService.createDarshan(templeId, request, authentication);
    }

    @GetMapping
    public List<DarshanResponse> listDarshans(
            @PathVariable long templeId,
            Authentication authentication) {
        return darshanService.listDarshans(templeId, authentication);
    }

    @GetMapping("/{darshanId}")
    public DarshanResponse getDarshan(
            @PathVariable long templeId,
            @PathVariable long darshanId,
            Authentication authentication) {
        return darshanService.getDarshan(templeId, darshanId, authentication);
    }

    @PatchMapping("/{darshanId}")
    public DarshanResponse updateDarshan(
            @PathVariable long templeId,
            @PathVariable long darshanId,
            @Valid @RequestBody UpdateDarshanRequest request,
            Authentication authentication) {
        return darshanService.updateDarshan(templeId, darshanId, request, authentication);
    }

    @PostMapping("/{darshanId}/slots")
    @ResponseStatus(HttpStatus.CREATED)
    public DarshanSlotResponse createSlot(
            @PathVariable long templeId,
            @PathVariable long darshanId,
            @Valid @RequestBody CreateDarshanSlotRequest request,
            Authentication authentication) {
        return darshanService.createSlot(templeId, darshanId, request, authentication);
    }

    @GetMapping("/{darshanId}/slots")
    public PageResponse<DarshanSlotResponse> listSlots(
            @PathVariable long templeId,
            @PathVariable long darshanId,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) OffsetDateTime from,
            @RequestParam(required = false) OffsetDateTime to,
            Authentication authentication) {
        return darshanService.listSlots(templeId, darshanId, page, size, date, from, to, authentication);
    }

    @GetMapping("/{darshanId}/slots/{slotId}")
    public DarshanSlotResponse getSlot(
            @PathVariable long templeId,
            @PathVariable long darshanId,
            @PathVariable long slotId,
            Authentication authentication) {
        return darshanService.getSlot(templeId, darshanId, slotId, authentication);
    }

    @PatchMapping("/{darshanId}/slots/{slotId}")
    public DarshanSlotResponse updateSlot(
            @PathVariable long templeId,
            @PathVariable long darshanId,
            @PathVariable long slotId,
            @Valid @RequestBody UpdateDarshanSlotRequest request,
            Authentication authentication) {
        return darshanService.updateSlot(templeId, darshanId, slotId, request, authentication);
    }
}
