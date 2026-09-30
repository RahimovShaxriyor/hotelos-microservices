package com.hotelos.reception.controller;

import com.hotelos.common.dto.PageResponse;
import com.hotelos.common.dto.guest.CreateGuestRequest;
import com.hotelos.common.dto.guest.GuestProfileResponse;
import com.hotelos.common.dto.guest.UpdateGuestRequest;
import com.hotelos.reception.service.GuestProfileService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/reception/guest-profiles")
public class GuestProfileController {

    private final GuestProfileService guestProfileService;

    public GuestProfileController(GuestProfileService guestProfileService) {
        this.guestProfileService = guestProfileService;
    }

    @PostMapping
    public ResponseEntity<GuestProfileResponse> createGuest(@Valid @RequestBody CreateGuestRequest request) {
        GuestProfileResponse response = guestProfileService.createGuest(request);
        URI location = URI.create("/api/reception/guests/" + response.id());
        return ResponseEntity.created(location).body(response);
    }

    @GetMapping
    public ResponseEntity<PageResponse<GuestProfileResponse>> listGuests(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) String search
    ) {
        PageResponse<GuestProfileResponse> response = guestProfileService.listGuests(page, size, sort, search);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{guestId}")
    public ResponseEntity<GuestProfileResponse> getGuest(@PathVariable UUID guestId) {
        GuestProfileResponse response = guestProfileService.getGuest(guestId);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{guestId}")
    public ResponseEntity<GuestProfileResponse> updateGuest(
            @PathVariable UUID guestId,
            @Valid @RequestBody UpdateGuestRequest request
    ) {
        GuestProfileResponse response = guestProfileService.updateGuest(guestId, request);
        return ResponseEntity.ok(response);
    }
}
