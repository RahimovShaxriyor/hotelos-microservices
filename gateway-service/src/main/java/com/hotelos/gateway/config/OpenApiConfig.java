package com.hotelos.gateway.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {
    @Bean
    public OpenAPI hotelOsOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("HotelOS API Gateway")
                        .version("1.0.0")
                        .description("Unified entry point for HotelOS microservices."))
                .servers(List.of(new Server().url("http://localhost:8090").description("Local Gateway")))
                .tags(List.of(
                        new io.swagger.v3.oas.models.tags.Tag().name("Authentication").description("Authentication and staff token operations"),
                        new io.swagger.v3.oas.models.tags.Tag().name("Reception").description("Guest check-in, check-out, and room management"),
                        new io.swagger.v3.oas.models.tags.Tag().name("Housekeeping").description("Housekeeping queue and room cleaning operations"),
                        new io.swagger.v3.oas.models.tags.Tag().name("Room Service").description("Room service orders and billing"),
                        new io.swagger.v3.oas.models.tags.Tag().name("Maintenance").description("Maintenance issues and technician operations"),
                        new io.swagger.v3.oas.models.tags.Tag().name("Dashboard").description("Real-time dashboard and event monitoring"),
                        new io.swagger.v3.oas.models.tags.Tag().name("Gateway").description("Gateway health, routing, and development utilities")
                ))
                .components(new Components()
                        .addSecuritySchemes("bearerAuth", new SecurityScheme()
                                .name("bearerAuth")
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }
}
