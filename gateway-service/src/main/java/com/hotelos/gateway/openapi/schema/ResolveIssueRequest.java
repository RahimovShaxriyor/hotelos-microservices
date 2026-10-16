package com.hotelos.gateway.openapi.schema;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "ResolveIssueRequest", description = "Optional payload when resolving a maintenance issue")
public record ResolveIssueRequest(
        @Schema(description = "Whether maintenance work requires subsequent housekeeping cleaning before room is sellable", example = "true")
        Boolean requiresCleaning
) {}
