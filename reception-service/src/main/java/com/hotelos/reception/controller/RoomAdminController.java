package com.hotelos.reception.controller;

import com.hotelos.common.dto.PageResponse;
import com.hotelos.reception.domain.RoomType;
import com.hotelos.reception.dto.CreateRoomRequest;
import com.hotelos.reception.dto.RoomAdminResponse;
import com.hotelos.reception.dto.RoomReconcileResponse;
import com.hotelos.reception.dto.UpdateRoomRequest;
import com.hotelos.reception.service.RoomAdminService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/rooms")
public class RoomAdminController {

    private final RoomAdminService roomAdminService;

    public RoomAdminController(RoomAdminService roomAdminService) {
        this.roomAdminService = roomAdminService;
    }

    @PostMapping
    public ResponseEntity<RoomAdminResponse> createRoom(@Valid @RequestBody CreateRoomRequest request) {
        RoomAdminResponse response = roomAdminService.createRoom(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<PageResponse<RoomAdminResponse>> listRooms(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) RoomType roomType,
            @RequestParam(required = false) Integer floor,
            @RequestParam(required = false) String search
    ) {
        return ResponseEntity.ok(roomAdminService.listRooms(page, size, active, roomType, floor, search));
    }

    @GetMapping("/{roomNumber}")
    public ResponseEntity<RoomAdminResponse> getRoom(@PathVariable String roomNumber) {
        return ResponseEntity.ok(roomAdminService.getRoom(roomNumber));
    }

    @PatchMapping("/{roomNumber}")
    public ResponseEntity<RoomAdminResponse> updateRoom(
            @PathVariable String roomNumber,
            @Valid @RequestBody UpdateRoomRequest request
    ) {
        return ResponseEntity.ok(roomAdminService.updateRoom(roomNumber, request));
    }

    @PostMapping("/{roomNumber}/activate")
    public ResponseEntity<RoomAdminResponse> activateRoom(@PathVariable String roomNumber) {
        return ResponseEntity.ok(roomAdminService.activateRoom(roomNumber));
    }

    @PostMapping("/{roomNumber}/deactivate")
    public ResponseEntity<RoomAdminResponse> deactivateRoom(@PathVariable String roomNumber) {
        return ResponseEntity.ok(roomAdminService.deactivateRoom(roomNumber));
    }

    @PostMapping("/{roomNumber}/reconcile-occupancy")
    public ResponseEntity<RoomReconcileResponse> reconcileOccupancy(@PathVariable String roomNumber) {
        return ResponseEntity.ok(roomAdminService.reconcileOccupancy(roomNumber));
    }
}
