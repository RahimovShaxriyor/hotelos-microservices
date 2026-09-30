package com.hotelos.reception.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.hotelos.reception.dto.CreateReservationRequest;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

@Component
public class ReservationFingerprintService {
    private final ObjectMapper objectMapper;

    public ReservationFingerprintService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String fingerprint(CreateReservationRequest request, String currency, String assignedRoomNumber) {
        ObjectNode canonical = objectMapper.createObjectNode();
        canonical.put("version", 1);
        canonical.put("guestId", request.guestId().toString().toLowerCase());
        canonical.put("roomType", request.roomType().name());
        canonical.put("arrivalDate", request.arrivalDate().toString());
        canonical.put("departureDate", request.departureDate().toString());
        if (assignedRoomNumber == null) canonical.putNull("assignedRoomNumber");
        else canonical.put("assignedRoomNumber", assignedRoomNumber);
        canonical.put("nightlyRateSnapshot", request.nightlyRateSnapshot().stripTrailingZeros().toPlainString());
        canonical.put("currency", currency);
        try {
            byte[] json = objectMapper.writeValueAsBytes(canonical);
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(json));
        } catch (JsonProcessingException | NoSuchAlgorithmException ex) {
            throw new IllegalStateException("Unable to calculate reservation request fingerprint", ex);
        }
    }
}
