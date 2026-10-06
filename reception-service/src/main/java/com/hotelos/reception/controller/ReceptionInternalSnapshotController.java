package com.hotelos.reception.controller;

import com.hotelos.reception.dto.RoomSnapshotDto;
import com.hotelos.reception.persistence.repository.RoomRepository;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/reception/internal/rooms")
public class ReceptionInternalSnapshotController {

    private final RoomRepository roomRepository;

    public ReceptionInternalSnapshotController(RoomRepository roomRepository) {
        this.roomRepository = roomRepository;
    }

    @GetMapping("/snapshot")
    public ResponseEntity<List<RoomSnapshotDto>> getRoomsSnapshot() {
        List<RoomSnapshotDto> snapshot = roomRepository.findAll(Sort.by("roomNumber")).stream()
                .map(room -> new RoomSnapshotDto(
                        room.getRoomNumber(),
                        room.getOccupancyStatus() != null ? room.getOccupancyStatus().name() : null,
                        room.getOccupancyChangedAt(),
                        room.isTurnoverPending(),
                        room.getTurnoverCorrelationId(),
                        room.getTurnoverChangedAt()
                ))
                .toList();
        return ResponseEntity.ok(snapshot);
    }
}
