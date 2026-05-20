package com.hotelos.reception.dto;

import java.time.Instant;

public record ErrorResponse(String message, Instant timestamp) { }
