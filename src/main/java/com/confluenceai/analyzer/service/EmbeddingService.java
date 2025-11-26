package com.confluenceai.analyzer.service;

import java.util.List;

public interface EmbeddingService {
    
    /**
     * Generate embedding for a single text
     */
    List<Float> generateEmbedding(String text);
    
    /**
     * Generate embeddings for multiple texts (batch processing)
     */
    List<List<Float>> generateEmbeddings(List<String> texts);
    
    /**
     * Get the dimension of embeddings
     */
    int getEmbeddingDimension();
}
