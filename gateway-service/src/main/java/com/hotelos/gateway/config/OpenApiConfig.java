package com.hotelos.gateway.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.*;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.tags.Tag;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;

import java.util.Collections;
import java.util.List;
import java.util.Set;

@Configuration
public class OpenApiConfig {

    private static final String SECURITY_SCHEME_NAME = "staffBearerAuth";

    private static final Set<String> PUBLIC_PATHS = Set.of(
            "/api/auth/login",
            "/api/auth/refresh",
            "/api/auth/logout",
            "/api/auth/invitations/accept",
            "/api/gateway/health",
            "/api/gateway/info"
    );

    @Bean
    public OpenAPI hotelOsOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("HotelOS API")
                        .version("1.0.0")
                        .description("Hotel Property Management API"))
                .servers(List.of(new Server().url("http://localhost:8090").description("HotelOS Gateway")))
                .tags(List.of(
                        new Tag().name("Authentication").description("Staff authentication, token lifecycle, and invitation acceptance"),
                        new Tag().name("Staff Administration").description("Staff identity governance, invitations, lifecycle management, and role assignment"),
                        new Tag().name("Room Administration").description("Physical room inventory lifecycle, administrative updates, and safe occupancy reconciliation"),
                        new Tag().name("Reception — Reservations").description("Room reservation booking, availability search, modifications, cancellation, and room assignment"),
                        new Tag().name("Reception — Guests").description("Guest stay records, guest profiles, and guest profile management"),
                        new Tag().name("Reception — Rooms").description("Room inventory, physical room states, and sellability status"),
                        new Tag().name("Reception — Stays").description("Guest check-in, check-out, stay extensions, and room transfers"),
                        new Tag().name("Reception — Folio").description("Guest accounting folios, charge and adjustment line-items, and bill calculations"),
                        new Tag().name("Housekeeping").description("Room turnover cleaning queue, cleaning task lifecycle, and defect reporting"),
                        new Tag().name("Maintenance").description("Room repair tickets, priority queue management, and issue resolution"),
                        new Tag().name("Room Service").description("Guest meal and amenity orders and room service charges"),
                        new Tag().name("Dashboard").description("Aggregated hotel operations snapshot, event feed, and WebSocket streaming tickets"),
                        new Tag().name("System").description("Gateway health probes, routing table, and runtime metadata"),
                        new Tag().name("Development / Demo").description("Development and demonstration workflows only — not part of production client API")
                ))
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME))
                .components(new Components()
                        .addSecuritySchemes(SECURITY_SCHEME_NAME, new SecurityScheme()
                                .name(SECURITY_SCHEME_NAME)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("HotelOS STAFF access token."))
                        .addSchemas("FieldErrorDto", createFieldErrorDtoSchema())
                        .addSchemas("ApiErrorResponse", createApiErrorResponseSchema())
                );
    }

    @Bean
    public OpenApiCustomizer openApiCustomizer() {
        return openApi -> {
            if (openApi.getPaths() == null) return;

            openApi.getPaths().forEach((path, pathItem) -> {
                boolean isPublic = PUBLIC_PATHS.contains(path);

                pathItem.readOperations().forEach(operation -> {
                    // 1. Centralized X-Correlation-ID request header parameter
                    boolean hasCorrelation = operation.getParameters() != null && operation.getParameters().stream()
                            .anyMatch(p -> "X-Correlation-ID".equalsIgnoreCase(p.getName()));
                    if (!hasCorrelation) {
                        operation.addParametersItem(new Parameter()
                                .in("header")
                                .name("X-Correlation-ID")
                                .description("Optional client request correlation identifier. Echoed in response and error payloads.")
                                .required(false)
                                .schema(new StringSchema().example("7cdcb4df-c453-4fc7-9735-f4fd73f81122")));
                    }

                    // 2. Normalize "*/*" content type to "application/json"
                    ApiResponses responses = operation.getResponses();
                    if (responses != null) {
                        responses.values().forEach(apiResp -> {
                            Content content = apiResp.getContent();
                            if (content != null && content.containsKey("*/*") && !content.containsKey(MediaType.APPLICATION_JSON_VALUE)) {
                                io.swagger.v3.oas.models.media.MediaType starMedia = content.remove("*/*");
                                content.addMediaType(MediaType.APPLICATION_JSON_VALUE, starMedia);
                            }
                        });
                    }

                    // 3. Security requirement alignment
                    if (isPublic) {
                        operation.setSecurity(Collections.emptyList());
                    } else {
                        operation.setSecurity(List.of(new SecurityRequirement().addList(SECURITY_SCHEME_NAME)));

                        // 4. Centralized 401 & 403 error response documentation on protected routes
                        if (responses != null) {
                            if (!responses.containsKey("401")) {
                                responses.addApiResponse("401", createErrorApiResponse("UNAUTHORIZED"));
                            }
                            if (!responses.containsKey("403")) {
                                responses.addApiResponse("403", createErrorApiResponse("ACCESS_DENIED"));
                            }
                        }
                    }
                });
            });
        };
    }

    private ApiResponse createErrorApiResponse(String description) {
        Content content = new Content().addMediaType(
                MediaType.APPLICATION_JSON_VALUE,
                new io.swagger.v3.oas.models.media.MediaType()
                        .schema(new Schema<>().$ref("#/components/schemas/ApiErrorResponse"))
        );
        return new ApiResponse().description(description).content(content);
    }

    private Schema<?> createFieldErrorDtoSchema() {
        return new ObjectSchema()
                .description("Validation error detail for specific request field")
                .addProperty("field", new StringSchema().description("Request field name").example("email"))
                .addProperty("message", new StringSchema().description("Validation constraint violation description").example("must be a well-formed email address"))
                .addProperty("rejectedValue", new StringSchema().description("Rejected input value").example("invalid-email"));
    }

    private Schema<?> createApiErrorResponseSchema() {
        return new ObjectSchema()
                .description("Canonical HotelOS API error envelope")
                .addProperty("code", new StringSchema().description("HotelOS business error code").example("USER_NOT_FOUND"))
                .addProperty("status", new IntegerSchema().description("HTTP status code").example(404))
                .addProperty("error", new StringSchema().description("HTTP error reason phrase").example("Not Found"))
                .addProperty("message", new StringSchema().description("Human-readable error narrative").example("User not found"))
                .addProperty("correlationId", new StringSchema().description("Request correlation ID").example("7cdcb4df-c453-4fc7-9735-f4fd73f81122"))
                .addProperty("timestamp", new DateTimeSchema().description("Error occurrence timestamp").example("2026-10-13T10:00:00Z"))
                .addProperty("path", new StringSchema().description("Request URI path").example("/api/admin/users/00000000-0000-0000-0000-000000000001"))
                .addProperty("fieldErrors", new ArraySchema().description("Validation field violations if applicable").items(new Schema<>().$ref("#/components/schemas/FieldErrorDto")));
    }
}
