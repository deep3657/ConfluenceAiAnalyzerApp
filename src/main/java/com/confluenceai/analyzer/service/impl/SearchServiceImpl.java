package com.confluenceai.analyzer.service.impl;

import com.confluenceai.analyzer.dto.ParsedRcaDto;
import com.confluenceai.analyzer.dto.SearchResult;
import com.confluenceai.analyzer.entity.RcaEmbedding;
import com.confluenceai.analyzer.entity.RcaPage;
import com.confluenceai.analyzer.repository.ParsedRcaRepository;
import com.confluenceai.analyzer.repository.RcaEmbeddingRepository;
import com.confluenceai.analyzer.repository.RcaPageRepository;
import com.confluenceai.analyzer.service.EmbeddingService;
import com.confluenceai.analyzer.service.SearchService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class SearchServiceImpl implements SearchService {
    
    private static final Logger logger = LoggerFactory.getLogger(SearchServiceImpl.class);
    
    private final EmbeddingService embeddingService;
    private final RcaEmbeddingRepository embeddingRepository;
    private final RcaPageRepository pageRepository;
    private final ParsedRcaRepository parsedRcaRepository;
    private final double minSimilarityScore;
    
    public SearchServiceImpl(
            EmbeddingService embeddingService,
            RcaEmbeddingRepository embeddingRepository,
            RcaPageRepository pageRepository,
            ParsedRcaRepository parsedRcaRepository,
            @Value("${search.min-similarity-score:0.7}") double minSimilarityScore) {
        this.embeddingService = embeddingService;
        this.embeddingRepository = embeddingRepository;
        this.pageRepository = pageRepository;
        this.parsedRcaRepository = parsedRcaRepository;
        this.minSimilarityScore = minSimilarityScore;
    }
    
    @Override
    public List<SearchResult> searchSimilarRCAs(String query, int topK, String userId) {
        // Generate embedding for query
        List<Float> queryEmbedding = embeddingService.generateEmbedding(query);
        if (queryEmbedding.isEmpty()) {
            logger.warn("Failed to generate embedding for query: {}", query);
            return new ArrayList<>();
        }
        
        // Convert to vector string format
        String vectorString = formatVector(queryEmbedding);
        
        // Calculate max distance from similarity score (1 - similarity = distance)
        double maxDistance = 1.0 - minSimilarityScore;
        
        // Extract primary keyword for hybrid search (use first significant word)
        String keyword = extractKeyword(query);
        
        // Perform hybrid search (vector + keyword)
        List<Object[]> results = embeddingRepository.findHybridSearch(
                vectorString, keyword, maxDistance, topK * 2); // Fetch more to filter
        
        return convertToHybridSearchResults(results, topK);
    }
    
    /**
     * Extract the most significant keyword from the query for hybrid search
     */
    private String extractKeyword(String query) {
        // Simple extraction: use the longest word that's not a common stop word
        String[] words = query.toLowerCase().split("\\s+");
        String keyword = "";
        for (String word : words) {
            if (word.length() > keyword.length() && !isStopWord(word)) {
                keyword = word;
            }
        }
        return keyword.isEmpty() ? query : keyword;
    }
    
    private boolean isStopWord(String word) {
        return List.of("the", "a", "an", "in", "on", "at", "for", "to", "of", "and", "or", "is", "was", "were", "are", "with", "from", "by").contains(word);
    }
    
    @Override
    public List<SearchResult> searchBySymptoms(String symptoms, int topK) {
        return searchSimilarRCAs(symptoms, topK, null);
    }
    
    @Override
    public List<SearchResult> searchByRootCause(String rootCause, int topK) {
        // Generate embedding
        List<Float> queryEmbedding = embeddingService.generateEmbedding(rootCause);
        if (queryEmbedding.isEmpty()) {
            return new ArrayList<>();
        }
        
        String vectorString = formatVector(queryEmbedding);
        double maxDistance = 1.0 - minSimilarityScore;
        
        // Search only in ROOT_CAUSE chunks
        List<Object[]> results = embeddingRepository.findSimilarEmbeddingsWithFilters(
                vectorString, maxDistance, "ROOT_CAUSE", null, topK);
        
        return convertToSearchResults(results);
    }
    
    private List<SearchResult> convertToSearchResults(List<Object[]> rawResults) {
        return rawResults.stream()
                .map(this::convertToSearchResult)
                .filter(result -> result.getSimilarityScore() >= minSimilarityScore)
                .collect(Collectors.toList());
    }
    
    private List<SearchResult> convertToHybridSearchResults(List<Object[]> rawResults, int topK) {
        // Hybrid query returns: all entity columns + vector_similarity + keyword_boost + combined_score
        // Indices: 0-8 = entity columns, 9 = vector_similarity, 10 = keyword_boost, 11 = combined_score
        return rawResults.stream()
                .map(this::convertHybridToSearchResult)
                .filter(result -> result.getSimilarityScore() >= minSimilarityScore * 0.8) // Slightly lower threshold for hybrid
                .limit(topK)
                .collect(Collectors.toList());
    }
    
    private SearchResult convertHybridToSearchResult(Object[] row) {
        // Hybrid query columns: id, page_id, chunk_index, chunk_type, content, embedding, metadata, created_at, updated_at, 
        //                       vector_similarity, keyword_boost, combined_score
        String pageId = (String) row[1];
        String chunkType = (String) row[3];
        String content = (String) row[4];
        Double combinedScore = ((Number) row[11]).doubleValue(); // Use combined score
        
        RcaPage page = pageRepository.findByPageId(pageId).orElse(null);
        
        ParsedRcaDto parsedRca = null;
        var parsedRcaOpt = parsedRcaRepository.findByPageId(pageId);
        if (parsedRcaOpt.isPresent()) {
            var pr = parsedRcaOpt.get();
            parsedRca = new ParsedRcaDto();
            parsedRca.setPageId(pr.getPageId());
            parsedRca.setSymptoms(pr.getSymptoms());
            parsedRca.setRootCause(pr.getRootCause());
            parsedRca.setResolution(pr.getResolution());
            parsedRca.setIncidentDate(pr.getIncidentDate());
        }
        
        SearchResult result = new SearchResult();
        result.setPageId(pageId);
        result.setTitle(page != null ? page.getTitle() : "");
        result.setContent(content);
        result.setConfluenceUrl(page != null ? page.getUrl() : "");
        result.setSimilarityScore(combinedScore);
        result.setChunkType(chunkType);
        result.setFullRCA(parsedRca);
        return result;
    }
    
    private SearchResult convertToSearchResult(Object[] row) {
        // Native query returns columns: id, page_id, chunk_index, chunk_type, content, embedding, metadata, created_at, updated_at, similarity
        // Indices: 0=id, 1=page_id, 2=chunk_index, 3=chunk_type, 4=content, 5=embedding, 6=metadata, 7=created_at, 8=updated_at, 9=similarity
        String pageId = (String) row[1];
        String chunkType = (String) row[3];
        String content = (String) row[4];
        Double similarity = ((Number) row[9]).doubleValue();
        
        RcaPage page = pageRepository.findByPageId(pageId).orElse(null);
        
        ParsedRcaDto parsedRca = null;
        var parsedRcaOpt = parsedRcaRepository.findByPageId(pageId);
        if (parsedRcaOpt.isPresent()) {
            var pr = parsedRcaOpt.get();
            parsedRca = new ParsedRcaDto();
            parsedRca.setPageId(pr.getPageId());
            parsedRca.setSymptoms(pr.getSymptoms());
            parsedRca.setRootCause(pr.getRootCause());
            parsedRca.setResolution(pr.getResolution());
            parsedRca.setIncidentDate(pr.getIncidentDate());
        }
        
        SearchResult result = new SearchResult();
        result.setPageId(pageId);
        result.setTitle(page != null ? page.getTitle() : "");
        result.setContent(content);
        result.setConfluenceUrl(page != null ? page.getUrl() : "");
        result.setSimilarityScore(similarity);
        result.setChunkType(chunkType);
        result.setFullRCA(parsedRca);
        return result;
    }
    
    private String formatVector(List<Float> vector) {
        return "[" + vector.stream()
                .map(f -> String.format("%.6f", f))
                .collect(Collectors.joining(",")) + "]";
    }
}

