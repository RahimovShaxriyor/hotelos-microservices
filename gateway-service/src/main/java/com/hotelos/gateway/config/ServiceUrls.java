package com.hotelos.gateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "hotelos.services")
public record ServiceUrls(
        String receptionUrl,
        String housekeepingUrl,
        String roomServiceUrl,
        String maintenanceUrl,
        String dashboardUrl
) {
}
