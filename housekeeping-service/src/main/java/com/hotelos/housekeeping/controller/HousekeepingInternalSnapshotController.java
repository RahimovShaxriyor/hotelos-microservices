package com.hotelos.housekeeping.controller;

import com.hotelos.housekeeping.dto.HousekeepingRoomSnapshotDto;
import com.hotelos.housekeeping.persistence.repository.RoomStateRepository;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/housekeeping/internal/rooms")
public class HousekeepingInternalSnapshotController {

    private final RoomStateRepository roomStateRepository;

    public HousekeepingInternalSnapshotController(RoomStateRepository roomStateRepository) {
        this.roomStateRepository = roomStateRepository;
    }

    @GetMapping("/snapshot")
    public ResponseEntity<List<HousekeepingRoomSnapshotDto>> getRoomsSnapshot() {
        List<HousekeepingRoomSnapshotDto> snapshot = roomStateRepository.findAll(Sort.by("roomNumber")).stream()
                .map(room -> new HousekeepingRoomSnapshotDto(
                        room.getRoomNumber(),
                        room.getHousekeepingStatus() != null ? room.getHousekeepingStatus().name() : null,
                        room.getStatusChangedAt()
                ))
                .toList();
        return ResponseEntity.ok(snapshot);
    }
}
