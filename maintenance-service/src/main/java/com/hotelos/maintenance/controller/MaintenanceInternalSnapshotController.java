package com.hotelos.maintenance.controller;

import com.hotelos.maintenance.dto.MaintenanceRoomSnapshotDto;
import com.hotelos.maintenance.persistence.repository.RoomStateRepository;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/maintenance/internal/rooms")
public class MaintenanceInternalSnapshotController {

    private final RoomStateRepository roomStateRepository;

    public MaintenanceInternalSnapshotController(RoomStateRepository roomStateRepository) {
        this.roomStateRepository = roomStateRepository;
    }

    @GetMapping("/snapshot")
    public ResponseEntity<List<MaintenanceRoomSnapshotDto>> getRoomsSnapshot() {
        List<MaintenanceRoomSnapshotDto> snapshot = roomStateRepository.findAll(Sort.by("roomNumber")).stream()
                .map(room -> new MaintenanceRoomSnapshotDto(
                        room.getRoomNumber(),
                        room.getEngineeringStatus() != null ? room.getEngineeringStatus().name() : null,
                        room.getStatusChangedAt()
                ))
                .toList();
        return ResponseEntity.ok(snapshot);
    }
}
