package io.github.tatame.aftersale.common.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Platform Health", description = "Lightweight platform health API. Actuator health remains separate.")
public class HealthController {

    @GetMapping("/api/health")
    @Operation(summary = "Get platform health", description = "Returns lightweight application health status.")
    public HealthResponse health() {
        return new HealthResponse("UP", "after-sale-agent-platform");
    }
}
