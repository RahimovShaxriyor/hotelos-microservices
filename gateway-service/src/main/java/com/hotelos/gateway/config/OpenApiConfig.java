package com.hotelos.gateway.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
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
                        .description("Single external entry point for HotelOS microservices. "
                                + "Use this Swagger UI to test Reception, Housekeeping, Room Service and Maintenance APIs through the gateway."))
                .servers(List.of(new Server().url("http://localhost:8090").description("Local Gateway")));
    }
}
