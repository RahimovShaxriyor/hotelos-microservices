package com.hotelos.dashboard.dto;

import java.util.Map;

public record BootstrapResultDto(
        String status,
        Map<String, Object> sources,
        int totalRoomsProjected
) {}
