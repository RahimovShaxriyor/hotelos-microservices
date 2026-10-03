package com.hotelos.reception.dto;

import com.hotelos.reception.persistence.entity.GuestStayEntity;
import com.hotelos.reception.persistence.entity.StayRoomHistoryEntity;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record StayResponse(
        UUID id,
        UUID guestId,
        UUID reservationId,
        String roomNumber,
        String guestName,
        String currency,
        LocalDate checkInDate,
        LocalDate departureDate,
        int bookedNights,
        BigDecimal nightlyRateAtCheckin,
        BigDecimal roomServiceCharges,
        BigDecimal totalBill,
        Instant checkedInAt,
        Instant checkedOutAt,
        boolean active,
        List<RoomHistorySegmentResponse> roomHistory
) {
    public record RoomHistorySegmentResponse(
            UUID id,
            String roomNumber,
            Instant startedAt,
            Instant endedAt,
            String reason
    ) {
        public static RoomHistorySegmentResponse from(StayRoomHistoryEntity entity) {
            return new RoomHistorySegmentResponse(
                    entity.getId(),
                    entity.getRoomNumber(),
                    entity.getStartedAt(),
                    entity.getEndedAt(),
                    entity.getReason()
            );
        }
    }

    public static StayResponse from(GuestStayEntity stay, List<StayRoomHistoryEntity> history,
                                    BigDecimal roomServiceCharges, BigDecimal totalBill) {
        LocalDate departureDate = stay.getCheckInDate().plusDays(stay.getBookedNights());
        List<RoomHistorySegmentResponse> historySegments = history != null
                ? history.stream().map(RoomHistorySegmentResponse::from).toList()
                : List.of();
        return new StayResponse(
                stay.getId(),
                stay.getGuestId(),
                stay.getReservationId(),
                stay.getRoomNumber(),
                stay.getGuestName(),
                stay.getCurrency(),
                stay.getCheckInDate(),
                departureDate,
                stay.getBookedNights(),
                stay.getNightlyRateAtCheckin(),
                roomServiceCharges != null ? roomServiceCharges : BigDecimal.ZERO,
                totalBill != null ? totalBill : BigDecimal.ZERO,
                stay.getCheckedInAt(),
                stay.getCheckedOutAt(),
                !stay.isCheckedOut(),
                historySegments
        );
    }
}
