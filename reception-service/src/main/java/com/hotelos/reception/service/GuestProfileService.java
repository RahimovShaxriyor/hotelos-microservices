package com.hotelos.reception.service;

import com.hotelos.common.dto.PageResponse;
import com.hotelos.common.dto.guest.CreateGuestRequest;
import com.hotelos.common.dto.guest.GuestProfileResponse;
import com.hotelos.common.dto.guest.UpdateGuestRequest;
import com.hotelos.reception.exception.HotelValidationException;
import com.hotelos.reception.exception.ResourceNotFoundException;
import com.hotelos.reception.persistence.entity.GuestEntity;
import com.hotelos.reception.persistence.repository.GuestRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class GuestProfileService {

    private final GuestRepository guestRepository;

    public GuestProfileService(GuestRepository guestRepository) {
        this.guestRepository = guestRepository;
    }

    @Transactional
    public GuestProfileResponse createGuest(CreateGuestRequest request) {
        if (request.getFirstName() == null || request.getFirstName().trim().isEmpty()) {
            throw new HotelValidationException("First name is required");
        }
        if (request.getLastName() == null || request.getLastName().trim().isEmpty()) {
            throw new HotelValidationException("Last name is required");
        }
        String email = request.getEmail() != null ? request.getEmail().trim().toLowerCase() : null;
        if (email != null && email.isEmpty()) {
            email = null;
        }
        String phone = request.getPhone() != null ? request.getPhone().trim() : null;
        if (phone != null && phone.isEmpty()) {
            phone = null;
        }

        GuestEntity guest = new GuestEntity(
                UUID.randomUUID(),
                request.getFirstName().trim(),
                request.getLastName().trim(),
                email,
                phone,
                Boolean.TRUE.equals(request.getVipStatus()),
                request.getNotes() != null ? request.getNotes().trim() : null
        );

        GuestEntity saved = guestRepository.save(guest);
        return mapToResponse(saved);
    }

    @Transactional(readOnly = true)
    public GuestProfileResponse getGuest(UUID id) {
        GuestEntity guest = guestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Guest profile with ID " + id + " not found"));
        return mapToResponse(guest);
    }

    @Transactional(readOnly = true)
    public PageResponse<GuestProfileResponse> listGuests(int page, int size, String sort, String search) {
        int validatedPage = Math.max(0, page);
        int validatedSize = Math.min(100, Math.max(1, size));

        Sort sortObj = Sort.by(Sort.Direction.ASC, "lastName");
        if (sort != null && !sort.isBlank()) {
            String[] parts = sort.split(",");
            String prop = parts[0].trim();
            Sort.Direction dir = (parts.length > 1 && "desc".equalsIgnoreCase(parts[1].trim()))
                    ? Sort.Direction.DESC
                    : Sort.Direction.ASC;
            if (List.of("lastName", "firstName", "createdAt", "updatedAt").contains(prop)) {
                sortObj = Sort.by(dir, prop);
            }
        }

        Pageable pageable = PageRequest.of(validatedPage, validatedSize, sortObj);
        Page<GuestEntity> guestPage;
        if (search != null && !search.trim().isEmpty()) {
            guestPage = guestRepository.searchGuests(search.trim(), pageable);
        } else {
            guestPage = guestRepository.findAll(pageable);
        }

        List<GuestProfileResponse> responses = guestPage.getContent().stream()
                .map(this::mapToResponse)
                .toList();

        return PageResponse.of(responses, guestPage.getNumber(), guestPage.getSize(), guestPage.getTotalElements());
    }

    @Transactional
    public GuestProfileResponse updateGuest(UUID id, UpdateGuestRequest request) {
        GuestEntity guest = guestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Guest profile with ID " + id + " not found"));

        if (request.getFirstName() == null || request.getFirstName().trim().isEmpty()) {
            throw new HotelValidationException("First name is required");
        }
        if (request.getLastName() == null || request.getLastName().trim().isEmpty()) {
            throw new HotelValidationException("Last name is required");
        }
        String email = request.getEmail() != null ? request.getEmail().trim().toLowerCase() : null;
        if (email != null && email.isEmpty()) {
            email = null;
        }
        String phone = request.getPhone() != null ? request.getPhone().trim() : null;
        if (phone != null && phone.isEmpty()) {
            phone = null;
        }

        guest.setFirstName(request.getFirstName().trim());
        guest.setLastName(request.getLastName().trim());
        guest.setEmail(email);
        guest.setPhone(phone);
        if (request.getVipStatus() != null) {
            guest.setVipStatus(request.getVipStatus());
        }
        if (request.getNotes() != null) {
            guest.setNotes(request.getNotes().trim());
        }

        GuestEntity saved = guestRepository.save(guest);
        return mapToResponse(saved);
    }

    public GuestProfileResponse mapToResponse(GuestEntity entity) {
        return new GuestProfileResponse(
                entity.getId(),
                entity.getFirstName(),
                entity.getLastName(),
                entity.getEmail(),
                entity.getPhone(),
                entity.isVipStatus(),
                entity.getNotes(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
