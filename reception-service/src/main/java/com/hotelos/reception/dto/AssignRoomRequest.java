package com.hotelos.reception.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AssignRoomRequest(@NotBlank @Size(max = 16) String roomNumber) { }
