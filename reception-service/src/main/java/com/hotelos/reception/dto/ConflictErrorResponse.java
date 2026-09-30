package com.hotelos.reception.dto;

import java.time.Instant;

public record ConflictErrorResponse(String code, String message, Instant timestamp) { }
