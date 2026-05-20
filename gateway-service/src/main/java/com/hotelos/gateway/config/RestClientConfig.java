package com.hotelos.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {
    @Bean
    public RestClient receptionClient(RestClient.Builder builder, ServiceUrls serviceUrls) {
        return builder.baseUrl(serviceUrls.receptionUrl()).build();
    }

    @Bean
    public RestClient housekeepingClient(RestClient.Builder builder, ServiceUrls serviceUrls) {
        return builder.baseUrl(serviceUrls.housekeepingUrl()).build();
    }

    @Bean
    public RestClient roomServiceClient(RestClient.Builder builder, ServiceUrls serviceUrls) {
        return builder.baseUrl(serviceUrls.roomServiceUrl()).build();
    }

    @Bean
    public RestClient maintenanceClient(RestClient.Builder builder, ServiceUrls serviceUrls) {
        return builder.baseUrl(serviceUrls.maintenanceUrl()).build();
    }

    @Bean
    public RestClient dashboardClient(RestClient.Builder builder, ServiceUrls serviceUrls) {
        return builder.baseUrl(serviceUrls.dashboardUrl()).build();
    }
}
