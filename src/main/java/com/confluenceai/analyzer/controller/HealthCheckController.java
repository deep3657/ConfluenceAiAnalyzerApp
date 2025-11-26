package com.confluenceai.analyzer.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Health check controller for monitoring application status.
 */
@RestController
@RequestMapping("/api")
@Tag(name = "Health", description = "Health check endpoints")
public class HealthCheckController {

    @Operation(
            summary = "Health check",
            description = "Returns the health status of the application with timestamp and service name."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Service is healthy")
    })
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("timestamp", LocalDateTime.now().toString());
        response.put("service", "Confluence AI Analyzer");
        return ResponseEntity.ok(response);
    }
}

