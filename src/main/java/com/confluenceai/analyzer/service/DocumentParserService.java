package com.confluenceai.analyzer.service;

import com.confluenceai.analyzer.dto.ConfluencePage;
import com.confluenceai.analyzer.dto.ParsedRcaDto;

import java.util.List;

public interface DocumentParserService {
    
    /**
     * Parse a Confluence page and extract structured RCA data
     */
    ParsedRcaDto parseDocument(ConfluencePage page);
    
    /**
     * Extract symptoms from content
     */
    List<String> extractSymptoms(String content);
    
    /**
     * Extract root cause from content
     */
    String extractRootCause(String content);
    
    /**
     * Extract resolution from content
     */
    String extractResolution(String content);
    
    /**
     * Chunk content into smaller pieces for embedding
     */
    List<String> chunkContent(String content, int chunkSize, int overlap);
}
