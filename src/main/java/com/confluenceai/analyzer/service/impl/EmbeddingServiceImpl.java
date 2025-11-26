package com.confluenceai.analyzer.service.impl;

import com.confluenceai.analyzer.service.EmbeddingService;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.theokanning.openai.service.OpenAiService;
import com.theokanning.openai.embedding.Embedding;
import com.theokanning.openai.embedding.EmbeddingRequest;
import okhttp3.OkHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class EmbeddingServiceImpl implements EmbeddingService {
    
    private static final Logger logger = LoggerFactory.getLogger(EmbeddingServiceImpl.class);
    
    private final OpenAiService openAiService;
    private final String model;
    private final int dimension;
    private final int batchSize;
    
    public EmbeddingServiceImpl(
            @Value("${embedding.model}") String model,
            @Value("${embedding.dimension}") int dimension,
            @Value("${embedding.batch-size}") int batchSize,
            @Value("${embedding.api-key}") String apiKey) {
        this.model = model;
        this.dimension = dimension;
        this.batchSize = batchSize;
        this.openAiService = new OpenAiService(apiKey);
    }
    
    @Override
    public List<Float> generateEmbedding(String text) {
        try {
            EmbeddingRequest request = EmbeddingRequest.builder()
                    .model(model)
                    .input(List.of(text))
                    .build();
            
            List<Embedding> embeddings = openAiService.createEmbeddings(request).getData();
            if (!embeddings.isEmpty()) {
                return embeddings.get(0).getEmbedding().stream()
                        .map(Double::floatValue)
                        .collect(Collectors.toList());
            }
        } catch (Exception e) {
            logger.error("Error generating embedding", e);
        }
        return new ArrayList<>();
    }
    
    @Override
    public List<List<Float>> generateEmbeddings(List<String> texts) {
        List<List<Float>> results = new ArrayList<>();
        
        // Process in batches
        for (int i = 0; i < texts.size(); i += batchSize) {
            int end = Math.min(i + batchSize, texts.size());
            List<String> batch = texts.subList(i, end);
            
            try {
                EmbeddingRequest request = EmbeddingRequest.builder()
                        .model(model)
                        .input(batch)
                        .build();
                
                List<Embedding> embeddings = openAiService.createEmbeddings(request).getData();
                for (Embedding embedding : embeddings) {
                    results.add(embedding.getEmbedding().stream()
                            .map(Double::floatValue)
                            .collect(Collectors.toList()));
                }
            } catch (Exception e) {
                logger.error("Error generating embeddings for batch", e);
                // Add empty lists for failed batch
                for (int j = 0; j < batch.size(); j++) {
                    results.add(new ArrayList<>());
                }
            }
        }
        
        return results;
    }
    
    @Override
    public int getEmbeddingDimension() {
        return dimension;
    }
}

