package com.hotelos.reception.dto;

import com.fasterxml.jackson.databind.JsonNode;
import com.hotelos.reception.domain.RoomType;
import com.hotelos.reception.exception.HotelValidationException;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

@Component
public class PatchReservationParser {
    private static final Set<String> MUTABLE_FIELDS = Set.of(
            "arrivalDate", "departureDate", "roomType", "assignedRoomNumber"
    );
    private static final Set<String> IMMUTABLE_FIELDS = Set.of(
            "guestId", "currency", "nightlyRateSnapshot", "status", "idempotencyKey", "requestFingerprint"
    );

    public PatchReservationCommand parse(JsonNode body) {
        if (body == null || !body.isObject()) {
            throw new HotelValidationException("PATCH body must be a JSON object");
        }
        if (body.isEmpty()) {
            throw new HotelValidationException("PATCH body must contain at least one mutable field");
        }

        Set<String> present = new HashSet<>();
        Iterator<String> names = body.fieldNames();
        while (names.hasNext()) {
            String name = names.next();
            if (IMMUTABLE_FIELDS.contains(name)) {
                throw new HotelValidationException("Field is immutable: " + name);
            }
            if (!MUTABLE_FIELDS.contains(name)) {
                throw new HotelValidationException("Unknown field: " + name);
            }
            if (body.get(name).isNull()) {
                throw new HotelValidationException("Explicit null is not allowed for field: " + name);
            }
            present.add(name);
        }

        return new PatchReservationCommand(
                optionalDate(body, present, "arrivalDate"),
                optionalDate(body, present, "departureDate"),
                optionalRoomType(body, present),
                optionalRoomNumber(body, present)
        );
    }

    private Optional<LocalDate> optionalDate(JsonNode body, Set<String> present, String name) {
        if (!present.contains(name)) return Optional.empty();
        if (!body.get(name).isTextual()) {
            throw new HotelValidationException("Field must be an ISO-8601 date: " + name);
        }
        try {
            return Optional.of(LocalDate.parse(body.get(name).textValue()));
        } catch (DateTimeParseException ex) {
            throw new HotelValidationException("Field must be an ISO-8601 date: " + name);
        }
    }

    private Optional<RoomType> optionalRoomType(JsonNode body, Set<String> present) {
        if (!present.contains("roomType")) return Optional.empty();
        if (!body.get("roomType").isTextual()) {
            throw new HotelValidationException("roomType must be a string");
        }
        try {
            return Optional.of(RoomType.valueOf(body.get("roomType").textValue().trim().toUpperCase(Locale.ROOT)));
        } catch (IllegalArgumentException ex) {
            throw new HotelValidationException("Unsupported room type: " + body.get("roomType").asText());
        }
    }

    private Optional<String> optionalRoomNumber(JsonNode body, Set<String> present) {
        if (!present.contains("assignedRoomNumber")) return Optional.empty();
        if (!body.get("assignedRoomNumber").isTextual()) {
            throw new HotelValidationException("assignedRoomNumber must be a string");
        }
        String value = body.get("assignedRoomNumber").textValue().trim();
        if (value.isEmpty() || value.length() > 16) {
            throw new HotelValidationException("assignedRoomNumber must contain 1 to 16 characters");
        }
        return Optional.of(value);
    }
}
