package com.hotelos.identity.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@Tag(name = "Authentication", description = "Staff authentication and token operations")
@RestController
@RequestMapping("/api/identity")
public class IdentityHealthController {

    @Operation(summary = "Identity service health check")
    @GetMapping("/health")
    public Map<String, Object> health() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("status", "UP");
        result.put("service", "identity-service");
        result.put("time", Instant.now().toString());
        return result;
    }
}
