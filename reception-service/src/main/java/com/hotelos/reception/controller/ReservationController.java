package com.hotelos.reception.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.hotelos.common.dto.PageResponse;
import com.hotelos.reception.domain.ReservationStatus;
import com.hotelos.reception.domain.RoomType;
import com.hotelos.reception.dto.*;
import com.hotelos.reception.service.ReservationService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api/reception/reservations")
public class ReservationController {
    private final ReservationService reservationService;
    private final PatchReservationParser patchParser;
    private final com.hotelos.reception.service.StayService stayService;

    public ReservationController(ReservationService reservationService,
                                 PatchReservationParser patchParser,
                                 com.hotelos.reception.service.StayService stayService) {
        this.reservationService = reservationService;
        this.patchParser = patchParser;
        this.stayService = stayService;
    }

    @PostMapping
    public ResponseEntity<ReservationResponse> create(
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody CreateReservationRequest request
    ) {
        ReservationService.ReservationCreationResult result = reservationService.create(request, idempotencyKey);
        URI location = URI.create("/api/reception/reservations/" + result.response().id());
        return result.created()
                ? ResponseEntity.created(location).body(result.response())
                : ResponseEntity.ok().location(location).body(result.response());
    }

    @GetMapping
    public ResponseEntity<PageResponse<ReservationResponse>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) ReservationStatus status,
            @RequestParam(required = false) UUID guestId,
            @RequestParam(required = false) RoomType roomType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate arrivalFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate arrivalTo,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate departureFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate departureTo,
            @RequestParam(required = false) String assignedRoomNumber
    ) {
        return ResponseEntity.ok(reservationService.list(
                page, size, sort, status, guestId, roomType,
                arrivalFrom, arrivalTo, departureFrom, departureTo, assignedRoomNumber
        ));
    }

    @GetMapping("/availability")
    public ResponseEntity<AvailabilityResponse> availability(
            @RequestParam RoomType roomType,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate arrivalDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate departureDate
    ) {
        return ResponseEntity.ok(reservationService.availability(roomType, arrivalDate, departureDate));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ReservationResponse> get(@PathVariable UUID id) {
        return ResponseEntity.ok(reservationService.get(id));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ReservationResponse> patch(@PathVariable UUID id, @RequestBody JsonNode body) {
        return ResponseEntity.ok(reservationService.patch(id, patchParser.parse(body)));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<ReservationResponse> cancel(@PathVariable UUID id) {
        return ResponseEntity.ok(reservationService.cancel(id));
    }

    @PostMapping("/{id}/no-show")
    public ResponseEntity<ReservationResponse> noShow(@PathVariable UUID id) {
        return ResponseEntity.ok(reservationService.markNoShow(id));
    }

    @PostMapping("/{id}/assign-room")
    public ResponseEntity<ReservationResponse> assignRoom(@PathVariable UUID id,
                                                          @Valid @RequestBody AssignRoomRequest request) {
        return ResponseEntity.ok(reservationService.assignRoom(id, request));
    }

    @PostMapping("/{id}/check-in")
    public ResponseEntity<StayResponse> checkIn(@PathVariable UUID id) {
        com.hotelos.reception.service.StayTransactionService.StayCreationResult result = stayService.checkInReservation(id);
        URI location = URI.create("/api/reception/stays/" + result.response().id());
        return result.created()
                ? ResponseEntity.created(location).body(result.response())
                : ResponseEntity.ok().location(location).body(result.response());
    }
}
