package com.confluenceai.analyzer.service;

import com.confluenceai.analyzer.dto.SearchResult;

import java.util.List;

public interface LLMService {
    
    /**
     * Generate summary from retrieved RCAs
     */
    String generateSummary(String userQuery, List<SearchResult> results);
    
    /**
     * Synthesize root cause from multiple RCAs
     */
    String synthesizeRootCause(List<SearchResult> results);
}
