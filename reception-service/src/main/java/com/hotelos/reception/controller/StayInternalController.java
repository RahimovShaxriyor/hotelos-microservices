package com.hotelos.reception.controller;

import com.hotelos.reception.dto.ActiveStayResolutionResponse;
import com.hotelos.reception.exception.ConflictException;
import com.hotelos.reception.exception.HotelValidationException;
import com.hotelos.reception.exception.NoActiveStayException;
import com.hotelos.reception.persistence.entity.GuestStayEntity;
import com.hotelos.reception.persistence.repository.GuestStayRepository;
import com.hotelos.reception.persistence.repository.RoomRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/reception/internal/active-stays")
public class StayInternalController {

    private final GuestStayRepository guestStayRepository;
    private final RoomRepository roomRepository;

    public StayInternalController(GuestStayRepository guestStayRepository, RoomRepository roomRepository) {
        this.guestStayRepository = guestStayRepository;
        this.roomRepository = roomRepository;
    }

    @GetMapping("/by-room/{roomNumber}")
    public ResponseEntity<ActiveStayResolutionResponse> getActiveStayByRoom(@PathVariable("roomNumber") String roomNumber) {
        if (roomNumber == null || roomNumber.isBlank()) {
            throw new HotelValidationException("Room number is required");
        }
        String normalizedRoom = roomNumber.trim();
        if (!roomRepository.existsById(normalizedRoom)) {
            throw new HotelValidationException("Invalid room number: " + normalizedRoom);
        }

        List<GuestStayEntity> activeStays = guestStayRepository.findAllByRoomNumberAndCheckedOutAtIsNull(normalizedRoom);
        if (activeStays.isEmpty()) {
            throw new NoActiveStayException("No active stay found for room: " + normalizedRoom);
        }
        if (activeStays.size() > 1) {
            throw new ConflictException("INTEGRITY_ANOMALY", "Multiple active stays found for room: " + normalizedRoom);
        }

        GuestStayEntity stay = activeStays.get(0);
        return ResponseEntity.ok(new ActiveStayResolutionResponse(stay.getId(), stay.getRoomNumber(), stay.getCurrency()));
    }
}
