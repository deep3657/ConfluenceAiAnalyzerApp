package com.confluenceai.analyzer.controller;

import com.confluenceai.analyzer.entity.RcaPage;
import com.confluenceai.analyzer.repository.RcaPageRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Management", description = "Management and monitoring endpoints")
public class ManagementController {
    
    private final RcaPageRepository rcaPageRepository;
    private final String defaultSpaces;
    private final String defaultTags;
    
    public ManagementController(
            RcaPageRepository rcaPageRepository,
            @Value("${confluence.spaces:}") String defaultSpaces,
            @Value("${confluence.tags:}") String defaultTags) {
        this.rcaPageRepository = rcaPageRepository;
        this.defaultSpaces = defaultSpaces;
        this.defaultTags = defaultTags;
    }
    
    @Operation(
            summary = "Get ingestion configuration",
            description = "Returns the default configuration for ingestion including spaces and tags."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Configuration retrieved successfully")
    })
    @GetMapping("/config/ingestion")
    public ResponseEntity<Map<String, Object>> getIngestionConfig() {
        Map<String, Object> config = new HashMap<>();
        config.put("spaces", Arrays.asList(defaultSpaces.split(",")));
        config.put("tags", Arrays.asList(defaultTags.split(",")));
        return ResponseEntity.ok(config);
    }
    
    @Operation(
            summary = "Get system statistics",
            description = "Returns system statistics including total pages and status breakdown."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Statistics retrieved successfully")
    })
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalPages", rcaPageRepository.count());
        stats.put("pagesByStatus", Map.of(
                "PENDING", rcaPageRepository.findByStatus("PENDING").size(),
                "PARSED", rcaPageRepository.findByStatus("PARSED").size(),
                "EMBEDDED", rcaPageRepository.findByStatus("EMBEDDED").size(),
                "ERROR", rcaPageRepository.findByStatus("ERROR").size()
        ));
        return ResponseEntity.ok(stats);
    }
    
    @Operation(
            summary = "Get page by ID",
            description = "Retrieves metadata for a specific RCA page by its Confluence page ID."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Page found",
                    content = @Content(schema = @Schema(implementation = RcaPage.class))),
            @ApiResponse(responseCode = "404", description = "Page not found")
    })
    @GetMapping("/pages/{pageId}")
    public ResponseEntity<RcaPage> getPage(@PathVariable String pageId) {
        return rcaPageRepository.findByPageId(pageId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}

