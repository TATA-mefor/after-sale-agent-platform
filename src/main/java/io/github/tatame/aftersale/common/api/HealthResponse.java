package io.github.tatame.aftersale.common.api;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Lightweight platform health response.")
public record HealthResponse(
        @Schema(description = "Health status.", example = "UP")
        String status,
        @Schema(description = "Service name.", example = "after-sale-agent-platform")
        String service) {
}
