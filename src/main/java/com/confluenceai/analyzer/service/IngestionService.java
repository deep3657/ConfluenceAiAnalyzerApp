package com.confluenceai.analyzer.service;

import com.confluenceai.analyzer.dto.SyncRequest;
import com.confluenceai.analyzer.dto.SyncResponse;

import java.util.UUID;

public interface IngestionService {
    
    /**
     * Start a sync operation
     */
    SyncResponse startSync(SyncRequest request);
    
    /**
     * Get sync status
     */
    SyncResponse getSyncStatus(UUID syncId);
    
    /**
     * Ingest a specific page
     */
    void ingestPage(String pageId);
    
    /**
     * Process a page: parse, chunk, embed, and store
     */
    void processPage(String pageId);
}
