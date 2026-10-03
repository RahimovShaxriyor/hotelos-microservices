package com.hotelos.reception.controller;

import com.hotelos.common.dto.PageResponse;
import com.hotelos.reception.dto.ChangeRoomRequest;
import com.hotelos.reception.dto.CheckOutResponse;
import com.hotelos.reception.dto.ExtendStayRequest;
import com.hotelos.reception.dto.StayResponse;
import com.hotelos.reception.service.StayService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/reception/stays")
public class StayController {

    private final StayService stayService;

    public StayController(StayService stayService) {
        this.stayService = stayService;
    }

    @GetMapping
    public ResponseEntity<PageResponse<StayResponse>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) Boolean activeOnly,
            @RequestParam(required = false) UUID guestId,
            @RequestParam(required = false) String roomNumber
    ) {
        return ResponseEntity.ok(stayService.list(page, size, sort, activeOnly, guestId, roomNumber));
    }

    @GetMapping("/{stayId}")
    public ResponseEntity<StayResponse> get(@PathVariable UUID stayId) {
        return ResponseEntity.ok(stayService.get(stayId));
    }

    @PostMapping("/{stayId}/extend")
    public ResponseEntity<StayResponse> extend(
            @PathVariable UUID stayId,
            @Valid @RequestBody ExtendStayRequest request
    ) {
        return ResponseEntity.ok(stayService.extendStay(stayId, request));
    }

    @PostMapping("/{stayId}/change-room")
    public ResponseEntity<StayResponse> changeRoom(
            @PathVariable UUID stayId,
            @Valid @RequestBody ChangeRoomRequest request
    ) {
        return ResponseEntity.ok(stayService.changeRoom(stayId, request));
    }

    @PostMapping("/{stayId}/check-out")
    public ResponseEntity<CheckOutResponse> checkOut(@PathVariable UUID stayId) {
        return ResponseEntity.ok(stayService.checkOutStay(stayId));
    }
}
