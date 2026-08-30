package com.temple.platform.temple.api;

import com.temple.platform.temple.api.dto.CreateTempleAdminAssignmentRequest;
import com.temple.platform.temple.api.dto.CreateTempleEventRequest;
import com.temple.platform.temple.api.dto.CreateTempleRequest;
import com.temple.platform.temple.api.dto.PageResponse;
import com.temple.platform.temple.api.dto.TempleEventResponse;
import com.temple.platform.temple.api.dto.TempleResponse;
import com.temple.platform.temple.api.dto.UpdateTempleEventRequest;
import com.temple.platform.temple.api.dto.UpdateTempleRequest;
import com.temple.platform.temple.service.TempleService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/temples")
public class TempleController {

    private final TempleService templeService;

    public TempleController(TempleService templeService) {
        this.templeService = templeService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TempleResponse createTemple(
            @Valid @RequestBody CreateTempleRequest request,
            Authentication authentication) {
        return templeService.createTemple(request, authentication);
    }

    @GetMapping
    public List<TempleResponse> listTemples(Authentication authentication) {
        return templeService.listTemples(authentication);
    }

    @GetMapping("/{templeId}")
    public TempleResponse getTemple(@PathVariable long templeId, Authentication authentication) {
        return templeService.getTemple(templeId, authentication);
    }

    @PatchMapping("/{templeId}")
    public TempleResponse updateTemple(
            @PathVariable long templeId,
            @Valid @RequestBody UpdateTempleRequest request,
            Authentication authentication) {
        return templeService.updateTemple(templeId, request, authentication);
    }

    @PostMapping("/{templeId}/admins")
    @ResponseStatus(HttpStatus.CREATED)
    public void assignTempleAdmin(
            @PathVariable long templeId,
            @Valid @RequestBody CreateTempleAdminAssignmentRequest request,
            Authentication authentication) {
        templeService.assignTempleAdmin(templeId, request, authentication);
    }

    @DeleteMapping("/{templeId}/admins/{accountId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeTempleAdmin(
            @PathVariable long templeId,
            @PathVariable long accountId,
            Authentication authentication) {
        templeService.removeTempleAdmin(templeId, accountId, authentication);
    }

    @PostMapping("/{templeId}/events")
    @ResponseStatus(HttpStatus.CREATED)
    public TempleEventResponse createEvent(
            @PathVariable long templeId,
            @Valid @RequestBody CreateTempleEventRequest request,
            Authentication authentication) {
        return templeService.createEvent(templeId, request, authentication);
    }

    @GetMapping("/{templeId}/events")
    public PageResponse<TempleEventResponse> listEvents(
            @PathVariable long templeId,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            Authentication authentication) {
        return templeService.listEvents(templeId, page, size, authentication);
    }

    @GetMapping("/{templeId}/events/{eventId}")
    public TempleEventResponse getEvent(
            @PathVariable long templeId,
            @PathVariable long eventId,
            Authentication authentication) {
        return templeService.getEvent(templeId, eventId, authentication);
    }

    @PatchMapping("/{templeId}/events/{eventId}")
    public TempleEventResponse updateEvent(
            @PathVariable long templeId,
            @PathVariable long eventId,
            @Valid @RequestBody UpdateTempleEventRequest request,
            Authentication authentication) {
        return templeService.updateEvent(templeId, eventId, request, authentication);
    }
}
