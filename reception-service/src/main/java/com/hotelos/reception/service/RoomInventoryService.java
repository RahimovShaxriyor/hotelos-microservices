package com.hotelos.reception.service;

import com.hotelos.reception.config.RabbitConfig;
import com.hotelos.reception.domain.*;
import com.hotelos.reception.dto.*;
import com.hotelos.reception.event.*;
import com.hotelos.reception.exception.HotelValidationException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

@Service
public class RoomInventoryService {
    private final Map<String, Room> rooms = new ConcurrentHashMap<>();
    private final Map<String, GuestStay> activeStaysByRoom = new ConcurrentHashMap<>();
    private final Map<String, GuestStay> archivedStaysById = new ConcurrentHashMap<>();
    private final ReentrantLock assignmentLock = new ReentrantLock();
    private final RabbitTemplate rabbitTemplate;
    private final BillingService billingService;

    public RoomInventoryService(RabbitTemplate rabbitTemplate, BillingService billingService) {
        this.rabbitTemplate = rabbitTemplate;
        this.billingService = billingService;
        seedDemoData();
    }

    public synchronized Map<String, Object> resetAndSeed() {
        rooms.clear();
        activeStaysByRoom.clear();
        archivedStaysById.clear();
        seedDemoData();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("message", "Reception demo data has been reset");
        result.put("rooms", rooms.size());
        result.put("activeGuests", activeStaysByRoom.size());
        return result;
    }

    public Map<String, Object> seed() {
        return resetAndSeed();
    }

    private void seedDemoData() {
        seedRooms();
        seedExistingGuests();
    }

    private void seedRooms() {
        Instant now = Instant.now();
        add(new Room("101", 1, RoomType.SINGLE, "LIFT", new BigDecimal("90"), RoomStatus.CLEAN, now.minusSeconds(7200)));
        add(new Room("102", 1, RoomType.DOUBLE, "STAIRS", new BigDecimal("130"), RoomStatus.CLEAN, now.minusSeconds(3600)));
        add(new Room("103", 1, RoomType.SUITE, "LIFT", new BigDecimal("240"), RoomStatus.CLEAN, now.minusSeconds(5000)));
        add(new Room("104", 1, RoomType.ACCESSIBLE, "LIFT", new BigDecimal("120"), RoomStatus.CLEAN, now.minusSeconds(6000)));
        add(new Room("115", 1, RoomType.SINGLE, "STAIRS", new BigDecimal("90"), RoomStatus.CLEAN, now.minusSeconds(1000)));
        add(new Room("201", 2, RoomType.SINGLE, "LIFT", new BigDecimal("95"), RoomStatus.CLEAN, now.minusSeconds(8000)));
        add(new Room("202", 2, RoomType.DOUBLE, "LIFT", new BigDecimal("140"), RoomStatus.CLEAN, now.minusSeconds(9000)));
        add(new Room("204", 2, RoomType.DOUBLE, "STAIRS", new BigDecimal("140"), RoomStatus.OCCUPIED, now.minusSeconds(9000)));
        add(new Room("301", 3, RoomType.DOUBLE, "LIFT", new BigDecimal("150"), RoomStatus.OCCUPIED, now.minusSeconds(12000)));
        add(new Room("302", 3, RoomType.DOUBLE, "STAIRS", new BigDecimal("150"), RoomStatus.CLEAN, now.minusSeconds(15000)));
    }

    private void add(Room room) {
        rooms.put(room.getRoomNumber(), room);
    }

    private void seedExistingGuests() {
        activeStaysByRoom.put("204", new GuestStay("John Smith", "204", 2));
        activeStaysByRoom.put("301", new GuestStay("Sara Lee", "301", 3));
    }

    public List<Room> getRooms() {
        return rooms.values().stream().sorted(Comparator.comparing(Room::getRoomNumber)).toList();
    }

    public Room getRoom(String roomNumber) {
        validateKnownRoom(roomNumber);
        return rooms.get(roomNumber);
    }

    public List<Room> getAvailableRooms(String roomType, Integer floor) {
        return rooms.values().stream()
                .filter(room -> room.getStatus() == RoomStatus.CLEAN)
                .filter(room -> roomType == null || roomType.isBlank() || room.getType() == parseRoomType(roomType))
                .filter(room -> floor == null || room.getFloor() == floor)
                .sorted(Comparator.comparing(Room::getCleanSince).thenComparing(Room::getRoomNumber))
                .toList();
    }

    public List<GuestStay> getGuests() {
        return activeStaysByRoom.values().stream()
                .sorted(Comparator.comparing(GuestStay::getRoomNumber))
                .toList();
    }

    public GuestStay getGuestByRoom(String roomNumber) {
        validateKnownRoom(roomNumber);
        GuestStay stay = activeStaysByRoom.get(roomNumber);
        if (stay == null) {
            throw new HotelValidationException("No active guest in room " + roomNumber);
        }
        return stay;
    }

    public GuestStay archiveGuest(String guestId) {
        GuestStay stay = activeStaysByRoom.values().stream()
                .filter(item -> item.getStayId().equals(guestId))
                .findFirst()
                .orElseThrow(() -> new HotelValidationException("Unknown guest stay ID: " + guestId));
        stay.archive();
        archivedStaysById.put(stay.getStayId(), stay);
        return stay;
    }

    public Map<String, Object> calculateBillForRoom(String roomNumber) {
        validateKnownRoom(roomNumber);
        GuestStay stay = activeStaysByRoom.get(roomNumber);
        if (stay == null) {
            throw new HotelValidationException("No active guest stay for room " + roomNumber);
        }
        BigDecimal total = billingService.calculateBill(rooms.get(roomNumber), stay);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("roomNumber", roomNumber);
        result.put("guestName", stay.getGuestName());
        result.put("bookedNights", stay.getBookedNights());
        result.put("roomServiceCharges", stay.getRoomServiceCharges());
        result.put("total", total);
        return result;
    }

    public CheckInResponse checkIn(CheckInRequest request) {
        validateCheckIn(request);
        RoomType requestedType = parseRoomType(request.getRoomType());
        assignmentLock.lock();
        try {
            List<Room> typeAndClean = rooms.values().stream()
                    .filter(room -> room.getType() == requestedType)
                    .filter(room -> room.getStatus() == RoomStatus.CLEAN)
                    .collect(Collectors.toList());

            if (typeAndClean.isEmpty()) {
                throw new HotelValidationException("No rooms available for requested type");
            }

            List<Room> preferredFloorRooms = typeAndClean;
            if (request.getPreferredFloor() != null) {
                List<Room> sameFloor = typeAndClean.stream()
                        .filter(room -> room.getFloor() == request.getPreferredFloor())
                        .collect(Collectors.toList());
                if (!sameFloor.isEmpty()) {
                    preferredFloorRooms = sameFloor;
                }
            }

            Room selected = preferredFloorRooms.stream()
                    .min((left, right) -> compareByLongestCleanThenProximity(left, right, request.getProximityPreference()))
                    .orElseThrow(() -> new HotelValidationException("No rooms available"));

            selected.markOccupied();
            GuestStay stay = new GuestStay(request.getGuestName().trim(), selected.getRoomNumber(), request.getNights());
            activeStaysByRoom.put(selected.getRoomNumber(), stay);
            publishRoomStatus(selected.getRoomNumber(), RoomStatus.OCCUPIED);
            return new CheckInResponse(stay.getStayId(), stay.getGuestName(), selected.getRoomNumber(),
                    selected.getType().name(), selected.getStatus().name(), "Guest checked in successfully");
        } finally {
            assignmentLock.unlock();
        }
    }

    private int compareByLongestCleanThenProximity(Room left, Room right, String proximityPreference) {
        int cleanCompare = left.getCleanSince().compareTo(right.getCleanSince());
        if (cleanCompare != 0) {
            return cleanCompare;
        }
        if (proximityPreference == null || proximityPreference.isBlank()) {
            return left.getRoomNumber().compareTo(right.getRoomNumber());
        }
        boolean leftMatches = proximityPreference.equalsIgnoreCase(left.getProximity());
        boolean rightMatches = proximityPreference.equalsIgnoreCase(right.getProximity());
        if (leftMatches == rightMatches) {
            return left.getRoomNumber().compareTo(right.getRoomNumber());
        }
        return leftMatches ? -1 : 1;
    }

    public CheckOutResponse checkOut(String roomNumber) {
        validateKnownRoom(roomNumber);
        Room room = rooms.get(roomNumber);
        GuestStay stay = activeStaysByRoom.get(roomNumber);
        if (stay == null || stay.isCheckedOut()) {
            throw new HotelValidationException("No active guest stay for room " + roomNumber);
        }
        BigDecimal total = billingService.calculateBill(room, stay);
        stay.checkOut();
        archivedStaysById.put(stay.getStayId(), stay);
        activeStaysByRoom.remove(roomNumber);
        room.markDirty();
        rabbitTemplate.convertAndSend(RabbitConfig.EXCHANGE, RabbitConfig.ROOM_VACATED,
                new RoomVacatedEvent(roomNumber, Instant.now()));
        publishRoomStatus(roomNumber, RoomStatus.DIRTY);
        return new CheckOutResponse(roomNumber, stay.getGuestName(), total, "Guest checked out and room vacated event published");
    }

    @RabbitListener(queues = RabbitConfig.RECEPTION_ROOM_STATUS_QUEUE)
    public void onRoomStatusChanged(RoomStatusChangedEvent event) {
        Room room = rooms.get(event.roomNumber());
        if (room != null) {
            room.updateStatus(RoomStatus.valueOf(event.status()), event.changedAt());
        }
    }

    @RabbitListener(queues = RabbitConfig.RECEPTION_CHARGE_QUEUE)
    public void onRoomServiceCharge(RoomServiceChargeEvent event) {
        GuestStay stay = activeStaysByRoom.get(event.roomNumber());
        if (stay != null) {
            stay.addRoomServiceCharge(event.amount());
        }
    }

    private void publishRoomStatus(String roomNumber, RoomStatus status) {
        rabbitTemplate.convertAndSend(RabbitConfig.EXCHANGE, RabbitConfig.ROOM_STATUS_CHANGED,
                new RoomStatusChangedEvent(roomNumber, status.name(), Instant.now()));
    }

    private void validateCheckIn(CheckInRequest request) {
        if (request == null) {
            throw new HotelValidationException("Request body is required");
        }
        if (request.getGuestName() == null || request.getGuestName().isBlank()) {
            throw new HotelValidationException("Guest name is required");
        }
        if (request.getNights() <= 0) {
            throw new HotelValidationException("Nights must be greater than zero");
        }
        if (request.getRoomType() == null) {
            throw new HotelValidationException("Room type is required");
        }
        parseRoomType(request.getRoomType());
    }

    private RoomType parseRoomType(String roomType) {
        try {
            return RoomType.valueOf(roomType.toUpperCase(Locale.ROOT));
        } catch (Exception ex) {
            throw new HotelValidationException("Unsupported room type: " + roomType);
        }
    }

    private void validateKnownRoom(String roomNumber) {
        if (roomNumber == null || !rooms.containsKey(roomNumber)) {
            throw new HotelValidationException("Invalid room number: " + roomNumber);
        }
    }
}
