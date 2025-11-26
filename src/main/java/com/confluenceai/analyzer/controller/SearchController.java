package com.confluenceai.analyzer.controller;

import com.confluenceai.analyzer.dto.SearchRequest;
import com.confluenceai.analyzer.dto.SearchResponse;
import com.confluenceai.analyzer.dto.SearchResult;
import com.confluenceai.analyzer.service.LLMService;
import com.confluenceai.analyzer.service.SearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/search")
@Tag(name = "Search", description = "Semantic search endpoints for finding similar RCA documents")
public class SearchController {
    
    private static final Logger logger = LoggerFactory.getLogger(SearchController.class);
    
    private final SearchService searchService;
    private final LLMService llmService;
    private final int defaultTopK;
    
    public SearchController(
            SearchService searchService,
            LLMService llmService,
            @Value("${search.default-top-k:5}") int defaultTopK) {
        this.searchService = searchService;
        this.llmService = llmService;
        this.defaultTopK = defaultTopK;
    }
    
    @Operation(
            summary = "Search similar RCAs",
            description = "Performs semantic search to find historical RCA documents similar to the query. Returns top-K results with similarity scores and AI-generated summary."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Search completed successfully",
                    content = @Content(schema = @Schema(implementation = SearchResponse.class))),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping
    public ResponseEntity<SearchResponse> search(@RequestBody SearchRequest request) {
        long startTime = System.currentTimeMillis();
        
        try {
            int topK = request.getTopK() != null ? request.getTopK() : defaultTopK;
            String userId = "system"; // TODO: Get from authentication context
            
            List<SearchResult> results = searchService.searchSimilarRCAs(
                    request.getQuery(), topK, userId);
            
            // Generate LLM summary
            String summary = llmService.generateSummary(request.getQuery(), results);
            
            // Determine confidence based on similarity scores
            String confidence = determineConfidence(results);
            
            SearchResponse.Summary summaryObj = new SearchResponse.Summary();
            summaryObj.setSuggestedRootCause(summary);
            summaryObj.setConfidence(confidence);
            summaryObj.setSimilarIncidents(results.size());
            
            long executionTime = System.currentTimeMillis() - startTime;
            
            SearchResponse response = new SearchResponse();
            response.setQuery(request.getQuery());
            response.setResults(results);
            response.setSummary(summaryObj);
            response.setExecutionTimeMs(executionTime);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error processing search request", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @Operation(
            summary = "Search by symptoms",
            description = "Searches for RCAs based on symptom similarity. Focuses on matching symptoms from historical incidents."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Search completed successfully",
                    content = @Content(schema = @Schema(implementation = SearchResponse.class))),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping("/symptoms")
    public ResponseEntity<SearchResponse> searchBySymptoms(@RequestBody SearchRequest request) {
        long startTime = System.currentTimeMillis();
        
        try {
            int topK = request.getTopK() != null ? request.getTopK() : defaultTopK;
            List<SearchResult> results = searchService.searchBySymptoms(request.getQuery(), topK);
            
            String summary = llmService.generateSummary(request.getQuery(), results);
            String confidence = determineConfidence(results);
            
            SearchResponse.Summary summaryObj = new SearchResponse.Summary();
            summaryObj.setSuggestedRootCause(summary);
            summaryObj.setConfidence(confidence);
            summaryObj.setSimilarIncidents(results.size());
            
            long executionTime = System.currentTimeMillis() - startTime;
            
            SearchResponse response = new SearchResponse();
            response.setQuery(request.getQuery());
            response.setResults(results);
            response.setSummary(summaryObj);
            response.setExecutionTimeMs(executionTime);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error processing symptoms search", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @Operation(
            summary = "Search by root cause",
            description = "Searches for RCAs based on root cause similarity. Focuses on matching root causes from historical incidents."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Search completed successfully",
                    content = @Content(schema = @Schema(implementation = SearchResponse.class))),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping("/root-cause")
    public ResponseEntity<SearchResponse> searchByRootCause(@RequestBody SearchRequest request) {
        long startTime = System.currentTimeMillis();
        
        try {
            int topK = request.getTopK() != null ? request.getTopK() : defaultTopK;
            List<SearchResult> results = searchService.searchByRootCause(request.getQuery(), topK);
            
            String summary = llmService.synthesizeRootCause(results);
            String confidence = determineConfidence(results);
            
            SearchResponse.Summary summaryObj = new SearchResponse.Summary();
            summaryObj.setSuggestedRootCause(summary);
            summaryObj.setConfidence(confidence);
            summaryObj.setSimilarIncidents(results.size());
            
            long executionTime = System.currentTimeMillis() - startTime;
            
            SearchResponse response = new SearchResponse();
            response.setQuery(request.getQuery());
            response.setResults(results);
            response.setSummary(summaryObj);
            response.setExecutionTimeMs(executionTime);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error processing root cause search", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    private String determineConfidence(List<SearchResult> results) {
        if (results.isEmpty()) {
            return "Low";
        }
        
        double avgSimilarity = results.stream()
                .mapToDouble(SearchResult::getSimilarityScore)
                .average()
                .orElse(0.0);
        
        if (avgSimilarity >= 0.85) {
            return "High";
        } else if (avgSimilarity >= 0.75) {
            return "Medium";
        } else {
            return "Low";
        }
    }
}
