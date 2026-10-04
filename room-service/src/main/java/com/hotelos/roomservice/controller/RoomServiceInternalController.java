package com.hotelos.roomservice.controller;

import com.hotelos.roomservice.dto.FinalizeStayRequest;
import com.hotelos.roomservice.dto.FreezeStayResponse;
import com.hotelos.roomservice.dto.UnfreezeStayRequest;
import com.hotelos.roomservice.exception.HotelValidationException;
import com.hotelos.roomservice.service.StayOrderingSessionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/room-service/internal/stays")
public class RoomServiceInternalController {

    private final StayOrderingSessionService sessionService;

    public RoomServiceInternalController(StayOrderingSessionService sessionService) {
        this.sessionService = sessionService;
    }

    @PostMapping("/{stayId}/freeze")
    public ResponseEntity<FreezeStayResponse> freeze(@PathVariable("stayId") UUID stayId) {
        if (stayId == null) {
            throw new HotelValidationException("stayId is required");
        }
        FreezeStayResponse response = sessionService.freezeStay(stayId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{stayId}/finalize")
    public ResponseEntity<Map<String, Object>> finalize(
            @PathVariable("stayId") UUID stayId,
            @RequestBody FinalizeStayRequest request
    ) {
        if (stayId == null) {
            throw new HotelValidationException("stayId is required");
        }
        if (request == null || request.freezeId() == null) {
            throw new HotelValidationException("freezeId is required");
        }
        sessionService.finalizeStay(stayId, request.freezeId());
        return ResponseEntity.ok(Map.of("message", "Stay finalized successfully", "stayId", stayId.toString()));
    }

    @PostMapping("/{stayId}/unfreeze")
    public ResponseEntity<Map<String, Object>> unfreeze(
            @PathVariable("stayId") UUID stayId,
            @RequestBody UnfreezeStayRequest request
    ) {
        if (stayId == null) {
            throw new HotelValidationException("stayId is required");
        }
        if (request == null || request.freezeId() == null) {
            throw new HotelValidationException("freezeId is required");
        }
        sessionService.unfreezeStay(stayId, request.freezeId());
        return ResponseEntity.ok(Map.of("message", "Stay unfrozen successfully", "stayId", stayId.toString()));
    }
}
