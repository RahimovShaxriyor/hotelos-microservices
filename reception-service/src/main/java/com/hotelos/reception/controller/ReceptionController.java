package com.hotelos.reception.controller;

import com.hotelos.reception.domain.GuestStay;
import com.hotelos.reception.domain.Room;
import com.hotelos.reception.dto.CheckInRequest;
import com.hotelos.reception.dto.CheckInResponse;
import com.hotelos.reception.dto.CheckOutResponse;
import com.hotelos.reception.service.RoomInventoryService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/reception")
public class ReceptionController {
    private final RoomInventoryService roomInventoryService;

    public ReceptionController(RoomInventoryService roomInventoryService) {
        this.roomInventoryService = roomInventoryService;
    }

    @GetMapping("/rooms")
    public List<Room> rooms() {
        return roomInventoryService.getRooms();
    }

    @GetMapping("/rooms/{roomNumber}")
    public Room room(@PathVariable String roomNumber) {
        return roomInventoryService.getRoom(roomNumber);
    }

    @GetMapping("/rooms/available")
    public List<Room> availableRooms(@RequestParam(required = false) String roomType,
                                     @RequestParam(required = false) Integer floor) {
        return roomInventoryService.getAvailableRooms(roomType, floor);
    }

    @PostMapping("/check-in")
    public CheckInResponse checkIn(@RequestBody CheckInRequest request) {
        return roomInventoryService.checkIn(request);
    }

    @PostMapping("/check-out/{roomNumber}")
    public CheckOutResponse checkOut(@PathVariable String roomNumber) {
        return roomInventoryService.checkOut(roomNumber);
    }

    @GetMapping("/guests")
    public List<GuestStay> guests() {
        return roomInventoryService.getGuests();
    }

    @GetMapping("/guests/by-room/{roomNumber}")
    public GuestStay guestByRoom(@PathVariable String roomNumber) {
        return roomInventoryService.getGuestByRoom(roomNumber);
    }

    @PatchMapping("/guests/{guestId}/archive")
    public GuestStay archiveGuest(@PathVariable String guestId) {
        return roomInventoryService.archiveGuest(guestId);
    }

    @PostMapping("/bills/{roomNumber}/calculate")
    public Map<String, Object> calculateBill(@PathVariable String roomNumber) {
        return roomInventoryService.calculateBillForRoom(roomNumber);
    }

    @PostMapping("/dev/reset")
    public Map<String, Object> reset() {
        return roomInventoryService.resetAndSeed();
    }

    @PostMapping("/dev/seed")
    public Map<String, Object> seed() {
        return roomInventoryService.seed();
    }
}
