package com.confluenceai.analyzer.service;

import com.confluenceai.analyzer.dto.SearchResult;

import java.util.List;

public interface SearchService {
    
    /**
     * Search for similar RCAs based on query
     */
    List<SearchResult> searchSimilarRCAs(String query, int topK, String userId);
    
    /**
     * Search by symptoms
     */
    List<SearchResult> searchBySymptoms(String symptoms, int topK);
    
    /**
     * Search by root cause
     */
    List<SearchResult> searchByRootCause(String rootCause, int topK);
}
