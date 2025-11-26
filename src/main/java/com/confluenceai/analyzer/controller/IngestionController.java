package com.confluenceai.analyzer.controller;

import com.confluenceai.analyzer.dto.SyncRequest;
import com.confluenceai.analyzer.dto.SyncResponse;
import com.confluenceai.analyzer.service.IngestionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/ingestion")
@Tag(name = "Ingestion", description = "Endpoints for syncing and ingesting Confluence RCA pages")
public class IngestionController {
    
    private static final Logger logger = LoggerFactory.getLogger(IngestionController.class);
    
    private final IngestionService ingestionService;
    
    public IngestionController(IngestionService ingestionService) {
        this.ingestionService = ingestionService;
    }
    
    @Operation(
            summary = "Start sync operation",
            description = "Initiates a sync operation to fetch and process RCA pages from Confluence. Supports FULL and INCREMENTAL sync types."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "202", description = "Sync started successfully",
                    content = @Content(schema = @Schema(implementation = SyncResponse.class))),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping("/sync")
    public ResponseEntity<SyncResponse> startSync(@RequestBody SyncRequest request) {
        try {
            SyncResponse response = ingestionService.startSync(request);
            return ResponseEntity.accepted().body(response);
        } catch (Exception e) {
            logger.error("Error starting sync", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @Operation(
            summary = "Get sync status",
            description = "Retrieves the current status of a sync operation including progress metrics."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Sync status retrieved",
                    content = @Content(schema = @Schema(implementation = SyncResponse.class))),
            @ApiResponse(responseCode = "404", description = "Sync not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/sync/{syncId}")
    public ResponseEntity<SyncResponse> getSyncStatus(@PathVariable UUID syncId) {
        try {
            SyncResponse response = ingestionService.getSyncStatus(syncId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error getting sync status", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @Operation(
            summary = "Ingest a specific page",
            description = "Manually triggers ingestion for a specific Confluence page by page ID."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "202", description = "Page ingestion started"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping("/page/{pageId}")
    public ResponseEntity<Void> ingestPage(@PathVariable String pageId) {
        try {
            ingestionService.ingestPage(pageId);
            return ResponseEntity.accepted().build();
        } catch (Exception e) {
            logger.error("Error ingesting page", e);
            return ResponseEntity.internalServerError().build();
        }
    }
}
