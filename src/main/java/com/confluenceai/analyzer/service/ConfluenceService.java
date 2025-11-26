package com.confluenceai.analyzer.service;

import com.confluenceai.analyzer.dto.ConfluencePage;

import java.time.LocalDateTime;
import java.util.List;

public interface ConfluenceService {
    
    /**
     * Fetch RCA pages from specified Confluence spaces
     */
    List<ConfluencePage> fetchRCAPages(String spaceKey, List<String> tags);
    
    /**
     * Fetch a specific page by ID
     */
    ConfluencePage fetchPageById(String pageId);
    
    /**
     * Fetch pages modified since a given timestamp
     */
    List<ConfluencePage> fetchModifiedPagesSince(LocalDateTime lastSync, List<String> spaceKeys, List<String> tags);
    
    /**
     * Authenticate with Confluence API
     */
    boolean authenticate(String token);
    
    /**
     * Check if user has access to a space
     */
    boolean hasUserAccess(String userId, String spaceKey);
}
