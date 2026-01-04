package com.confluenceai.analyzer.service.impl;

import com.confluenceai.analyzer.dto.ConfluencePage;
import com.confluenceai.analyzer.dto.ParsedRcaDto;
import com.confluenceai.analyzer.dto.SyncRequest;
import com.confluenceai.analyzer.dto.SyncResponse;
import com.confluenceai.analyzer.entity.*;
import com.confluenceai.analyzer.repository.*;
import com.confluenceai.analyzer.service.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
public class IngestionServiceImpl implements IngestionService {
    
    private static final Logger logger = LoggerFactory.getLogger(IngestionServiceImpl.class);
    
    private final ConfluenceService confluenceService;
    private final DocumentParserService documentParserService;
    private final EmbeddingService embeddingService;
    private final RcaPageRepository rcaPageRepository;
    private final ParsedRcaRepository parsedRcaRepository;
    private final RcaEmbeddingRepository embeddingRepository;
    private final SyncHistoryRepository syncHistoryRepository;
    private final int chunkSize;
    private final int chunkOverlap;
    
    public IngestionServiceImpl(
            ConfluenceService confluenceService,
            DocumentParserService documentParserService,
            EmbeddingService embeddingService,
            RcaPageRepository rcaPageRepository,
            ParsedRcaRepository parsedRcaRepository,
            RcaEmbeddingRepository embeddingRepository,
            SyncHistoryRepository syncHistoryRepository,
            @Value("${chunking.size:800}") int chunkSize,
            @Value("${chunking.overlap:150}") int chunkOverlap) {
        this.confluenceService = confluenceService;
        this.documentParserService = documentParserService;
        this.embeddingService = embeddingService;
        this.rcaPageRepository = rcaPageRepository;
        this.parsedRcaRepository = parsedRcaRepository;
        this.embeddingRepository = embeddingRepository;
        this.syncHistoryRepository = syncHistoryRepository;
        this.chunkSize = chunkSize;
        this.chunkOverlap = chunkOverlap;
    }
    
    @Override
    public SyncResponse startSync(SyncRequest request) {
        // Create sync history record immediately and return
        SyncHistory syncHistory = new SyncHistory();
        syncHistory.setSyncType(request.getSyncType());
        syncHistory.setSpacesSynced(request.getSpaceKeys().toArray(new String[0]));
        syncHistory.setStatus("RUNNING");
        syncHistory.setStartedAt(LocalDateTime.now());
        syncHistory = syncHistoryRepository.save(syncHistory);
        
        // Start async processing
        performSyncAsync(syncHistory.getId(), request);
        
        // Return immediately with initial response
        SyncResponse response = convertToSyncResponse(syncHistory);
        response.setMessage("Sync started successfully");
        return response;
    }
    
    @Async
    public void performSyncAsync(UUID syncId, SyncRequest request) {
        SyncHistory syncHistory = syncHistoryRepository.findById(syncId)
                .orElseThrow(() -> new RuntimeException("Sync not found: " + syncId));
        
        try {
            int pagesFetched = 0;
            int pagesProcessed = 0;
            int pagesFailed = 0;
            
            LocalDateTime lastSync = null;
            if ("INCREMENTAL".equals(request.getSyncType())) {
                var lastSyncOpt = syncHistoryRepository.findFirstByOrderByStartedAtDesc();
                if (lastSyncOpt.isPresent() && !lastSyncOpt.get().getId().equals(syncId)) {
                    lastSync = lastSyncOpt.get().getStartedAt();
                }
            }
            
            for (String spaceKey : request.getSpaceKeys()) {
                List<ConfluencePage> pages;
                if (lastSync != null) {
                    pages = confluenceService.fetchModifiedPagesSince(
                            lastSync, List.of(spaceKey), request.getTags());
                } else {
                    pages = confluenceService.fetchRCAPages(spaceKey, request.getTags());
                }
                
                pagesFetched += pages.size();
                
                // Update progress
                syncHistory.setPagesFetched(pagesFetched);
                syncHistoryRepository.save(syncHistory);
                
                for (ConfluencePage page : pages) {
                    try {
                        // Save page first
                        savePageMetadata(page);
                        processPage(page.getId());
                        pagesProcessed++;
                    } catch (Exception e) {
                        logger.error("Error processing page {}", page.getId(), e);
                        pagesFailed++;
                    }
                    
                    // Update progress after each page
                    syncHistory.setPagesProcessed(pagesProcessed);
                    syncHistory.setPagesFailed(pagesFailed);
                    syncHistoryRepository.save(syncHistory);
                }
            }
            
            syncHistory.setPagesFetched(pagesFetched);
            syncHistory.setPagesProcessed(pagesProcessed);
            syncHistory.setPagesFailed(pagesFailed);
            syncHistory.setStatus("COMPLETED");
            syncHistory.setCompletedAt(LocalDateTime.now());
            syncHistoryRepository.save(syncHistory);
            
            logger.info("Sync {} completed. Fetched: {}, Processed: {}, Failed: {}", 
                    syncId, pagesFetched, pagesProcessed, pagesFailed);
            
        } catch (Exception e) {
            logger.error("Error during sync {}", syncId, e);
            syncHistory.setStatus("FAILED");
            syncHistory.setErrorMessage(e.getMessage());
            syncHistory.setCompletedAt(LocalDateTime.now());
            syncHistoryRepository.save(syncHistory);
        }
    }
    
    private void savePageMetadata(ConfluencePage page) {
        RcaPage rcaPage = rcaPageRepository.findByPageId(page.getId())
                .orElse(new RcaPage());
        rcaPage.setPageId(page.getId());
        rcaPage.setSpaceKey(page.getSpaceKey());
        rcaPage.setTitle(page.getTitle());
        rcaPage.setUrl(page.getUrl());
        rcaPage.setTags(page.getLabels() != null ? page.getLabels().toArray(new String[0]) : new String[0]);
        rcaPage.setLastModified(page.getLastModified());
        rcaPage.setStatus("PENDING");
        rcaPageRepository.save(rcaPage);
    }
    
    @Override
    public SyncResponse getSyncStatus(UUID syncId) {
        return syncHistoryRepository.findById(syncId)
                .map(this::convertToSyncResponse)
                .orElseThrow(() -> new RuntimeException("Sync not found: " + syncId));
    }
    
    @Override
    public void ingestPage(String pageId) {
        ConfluencePage page = confluenceService.fetchPageById(pageId);
        if (page == null) {
            throw new RuntimeException("Page not found: " + pageId);
        }
        
        // Save page metadata
        RcaPage rcaPage = new RcaPage();
        rcaPage.setPageId(page.getId());
        rcaPage.setSpaceKey(page.getSpaceKey());
        rcaPage.setTitle(page.getTitle());
        rcaPage.setUrl(page.getUrl());
        rcaPage.setTags(page.getLabels().toArray(new String[0]));
        rcaPage.setLastModified(page.getLastModified());
        rcaPage.setStatus("PENDING");
        rcaPageRepository.save(rcaPage);
        
        processPage(pageId);
    }
    
    @Override
    @Transactional
    public void processPage(String pageId) {
        RcaPage rcaPage = rcaPageRepository.findByPageId(pageId)
                .orElseThrow(() -> new RuntimeException("RCA page not found: " + pageId));
        
        try {
            // Fetch page content
            ConfluencePage page = confluenceService.fetchPageById(pageId);
            if (page == null) {
                throw new RuntimeException("Confluence page not found: " + pageId);
            }
            
            // Parse document
            ParsedRcaDto parsedRca = documentParserService.parseDocument(page);
            rcaPage.setParsedAt(LocalDateTime.now());
            rcaPage.setStatus("PARSED");
            rcaPageRepository.save(rcaPage);
            
            // Save parsed RCA
            ParsedRca parsedRcaEntity = new ParsedRca();
            parsedRcaEntity.setPageId(pageId);
            parsedRcaEntity.setSymptoms(parsedRca.getSymptoms());
            parsedRcaEntity.setRootCause(parsedRca.getRootCause());
            parsedRcaEntity.setResolution(parsedRca.getResolution());
            parsedRcaEntity.setIncidentDate(parsedRca.getIncidentDate());
            parsedRcaRepository.save(parsedRcaEntity);
            
            // Delete old embeddings
            embeddingRepository.deleteByPageId(pageId);
            
            // Chunk and embed symptoms
            if (parsedRca.getSymptoms() != null && !parsedRca.getSymptoms().isEmpty()) {
                List<String> symptomChunks = documentParserService.chunkContent(
                        parsedRca.getSymptoms(), chunkSize, chunkOverlap);
                createEmbeddings(pageId, symptomChunks, "SYMPTOMS");
            }
            
            // Chunk and embed root cause
            if (parsedRca.getRootCause() != null && !parsedRca.getRootCause().isEmpty()) {
                List<String> rootCauseChunks = documentParserService.chunkContent(
                        parsedRca.getRootCause(), chunkSize, chunkOverlap);
                createEmbeddings(pageId, rootCauseChunks, "ROOT_CAUSE");
            }
            
            rcaPage.setEmbeddingGeneratedAt(LocalDateTime.now());
            rcaPage.setStatus("EMBEDDED");
            rcaPageRepository.save(rcaPage);
            
        } catch (Exception e) {
            logger.error("Error processing page {}", pageId, e);
            rcaPage.setStatus("ERROR");
            rcaPage.setErrorMessage(e.getMessage());
            rcaPageRepository.save(rcaPage);
            throw e;
        }
    }
    
    private void createEmbeddings(String pageId, List<String> chunks, String chunkType) {
        if (chunks.isEmpty()) {
            return;
        }
        
        // Generate embeddings in batch
        List<List<Float>> embeddings = embeddingService.generateEmbeddings(chunks);
        
        // Save embeddings
            for (int i = 0; i < chunks.size(); i++) {
                if (i < embeddings.size() && !embeddings.get(i).isEmpty()) {
                    RcaEmbedding embedding = new RcaEmbedding();
                    embedding.setPageId(pageId);
                    embedding.setChunkIndex(i);
                    embedding.setChunkType(chunkType);
                    embedding.setContent(chunks.get(i));
                    embedding.setEmbeddingVector(embeddings.get(i));
                    embeddingRepository.save(embedding);
                }
            }
    }
    
    private SyncResponse convertToSyncResponse(SyncHistory syncHistory) {
        SyncResponse response = new SyncResponse();
        response.setSyncId(syncHistory.getId());
        response.setStatus(syncHistory.getStatus());
        response.setMessage(syncHistory.getErrorMessage() != null ? syncHistory.getErrorMessage() : "Sync " + syncHistory.getStatus().toLowerCase());
        response.setStartedAt(syncHistory.getStartedAt());
        response.setCompletedAt(syncHistory.getCompletedAt());
        response.setPagesFetched(syncHistory.getPagesFetched() != null ? syncHistory.getPagesFetched() : 0);
        response.setPagesProcessed(syncHistory.getPagesProcessed() != null ? syncHistory.getPagesProcessed() : 0);
        response.setPagesFailed(syncHistory.getPagesFailed() != null ? syncHistory.getPagesFailed() : 0);
        response.setEstimatedCompletionTime(syncHistory.getCompletedAt());
        return response;
    }
}

