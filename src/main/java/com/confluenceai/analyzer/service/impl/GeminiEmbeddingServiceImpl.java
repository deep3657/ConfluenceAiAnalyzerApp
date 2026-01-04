package com.confluenceai.analyzer.service.impl;

import com.confluenceai.analyzer.service.EmbeddingService;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@Primary
@ConditionalOnProperty(name = "embedding.provider", havingValue = "gemini", matchIfMissing = true)
public class GeminiEmbeddingServiceImpl implements EmbeddingService {
    
    private static final Logger logger = LoggerFactory.getLogger(GeminiEmbeddingServiceImpl.class);
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private static final String GEMINI_EMBED_URL = "https://generativelanguage.googleapis.com/v1beta/models/%s:embedContent?key=%s";
    private static final String GEMINI_BATCH_EMBED_URL = "https://generativelanguage.googleapis.com/v1beta/models/%s:batchEmbedContents?key=%s";
    
    private final OkHttpClient httpClient;
    private final Gson gson;
    private final String apiKey;
    private final String model;
    private final int dimension;
    private final int batchSize;
    
    public GeminiEmbeddingServiceImpl(
            @Value("${embedding.gemini.api-key:${GEMINI_API_KEY:}}") String apiKey,
            @Value("${embedding.gemini.model:text-embedding-004}") String model,
            @Value("${embedding.dimension:768}") int dimension,
            @Value("${embedding.batch-size:100}") int batchSize) {
        this.apiKey = apiKey;
        this.model = model;
        this.dimension = dimension;
        this.batchSize = batchSize;
        this.gson = new Gson();
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .build();
        
        logger.info("Gemini Embedding Service initialized - Model: {}, Dimension: {}", model, dimension);
    }
    
    @Override
    public List<Float> generateEmbedding(String text) {
        if (apiKey == null || apiKey.isEmpty()) {
            logger.warn("Gemini API key not configured. Set GEMINI_API_KEY environment variable.");
            return new ArrayList<>();
        }
        
        try {
            String url = String.format(GEMINI_EMBED_URL, model, apiKey);
            
            JsonObject content = new JsonObject();
            JsonArray parts = new JsonArray();
            JsonObject textPart = new JsonObject();
            textPart.addProperty("text", text);
            parts.add(textPart);
            content.add("parts", parts);
            
            JsonObject requestBody = new JsonObject();
            requestBody.add("content", content);
            requestBody.addProperty("taskType", "RETRIEVAL_DOCUMENT");
            
            Request request = new Request.Builder()
                    .url(url)
                    .post(RequestBody.create(gson.toJson(requestBody), JSON))
                    .header("Content-Type", "application/json")
                    .build();
            
            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    String errorBody = response.body() != null ? response.body().string() : "No error body";
                    logger.error("Gemini API error: {} - {}", response.code(), errorBody);
                    return new ArrayList<>();
                }
                
                String responseBody = response.body().string();
                JsonObject jsonResponse = gson.fromJson(responseBody, JsonObject.class);
                
                if (jsonResponse.has("embedding")) {
                    JsonArray values = jsonResponse.getAsJsonObject("embedding").getAsJsonArray("values");
                    List<Float> embedding = new ArrayList<>();
                    for (JsonElement val : values) {
                        embedding.add(val.getAsFloat());
                    }
                    return embedding;
                }
            }
        } catch (IOException e) {
            logger.error("Error generating Gemini embedding", e);
        }
        
        return new ArrayList<>();
    }
    
    @Override
    public List<List<Float>> generateEmbeddings(List<String> texts) {
        List<List<Float>> results = new ArrayList<>();
        
        if (apiKey == null || apiKey.isEmpty()) {
            logger.warn("Gemini API key not configured. Set GEMINI_API_KEY environment variable.");
            for (int i = 0; i < texts.size(); i++) {
                results.add(new ArrayList<>());
            }
            return results;
        }
        
        // Process in batches (Gemini supports up to 100 texts per batch)
        for (int i = 0; i < texts.size(); i += batchSize) {
            int end = Math.min(i + batchSize, texts.size());
            List<String> batch = texts.subList(i, end);
            
            try {
                List<List<Float>> batchResults = generateBatchEmbeddings(batch);
                results.addAll(batchResults);
            } catch (Exception e) {
                logger.error("Error generating embeddings for batch starting at {}", i, e);
                // Add empty lists for failed batch
                for (int j = 0; j < batch.size(); j++) {
                    results.add(new ArrayList<>());
                }
            }
        }
        
        return results;
    }
    
    private List<List<Float>> generateBatchEmbeddings(List<String> texts) throws IOException {
        String url = String.format(GEMINI_BATCH_EMBED_URL, model, apiKey);
        
        JsonArray requests = new JsonArray();
        for (String text : texts) {
            JsonObject embedRequest = new JsonObject();
            embedRequest.addProperty("model", "models/" + model);
            
            JsonObject content = new JsonObject();
            JsonArray parts = new JsonArray();
            JsonObject textPart = new JsonObject();
            textPart.addProperty("text", text);
            parts.add(textPart);
            content.add("parts", parts);
            
            embedRequest.add("content", content);
            embedRequest.addProperty("taskType", "RETRIEVAL_DOCUMENT");
            requests.add(embedRequest);
        }
        
        JsonObject requestBody = new JsonObject();
        requestBody.add("requests", requests);
        
        Request request = new Request.Builder()
                .url(url)
                .post(RequestBody.create(gson.toJson(requestBody), JSON))
                .header("Content-Type", "application/json")
                .build();
        
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String errorBody = response.body() != null ? response.body().string() : "No error body";
                logger.error("Gemini batch API error: {} - {}", response.code(), errorBody);
                throw new IOException("Gemini API error: " + response.code());
            }
            
            String responseBody = response.body().string();
            JsonObject jsonResponse = gson.fromJson(responseBody, JsonObject.class);
            
            List<List<Float>> results = new ArrayList<>();
            
            if (jsonResponse.has("embeddings")) {
                JsonArray embeddings = jsonResponse.getAsJsonArray("embeddings");
                for (JsonElement embeddingElement : embeddings) {
                    JsonObject embeddingObj = embeddingElement.getAsJsonObject();
                    JsonArray values = embeddingObj.getAsJsonArray("values");
                    List<Float> embedding = new ArrayList<>();
                    for (JsonElement val : values) {
                        embedding.add(val.getAsFloat());
                    }
                    results.add(embedding);
                }
            }
            
            return results;
        }
    }
    
    @Override
    public int getEmbeddingDimension() {
        return dimension;
    }
}

