package com.confluenceai.analyzer.service.impl;

import com.confluenceai.analyzer.dto.ConfluencePage;
import com.confluenceai.analyzer.service.ConfluenceService;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import okhttp3.*;
import okhttp3.logging.HttpLoggingInterceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.StreamSupport;

@Service
public class ConfluenceServiceImpl implements ConfluenceService {
    
    private static final Logger logger = LoggerFactory.getLogger(ConfluenceServiceImpl.class);
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    
    private final OkHttpClient httpClient;
    private final Gson gson;
    private final String baseUrl;
    private final String authToken;
    
    public ConfluenceServiceImpl(
            @Value("${confluence.base-url}") String baseUrl,
            @Value("${confluence.auth.token}") String authToken) {
        this.baseUrl = baseUrl;
        this.authToken = authToken;
        this.httpClient = new OkHttpClient.Builder()
                .addInterceptor(new HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BASIC))
                .build();
        this.gson = new Gson();
    }
    
    @Override
    public List<ConfluencePage> fetchRCAPages(String spaceKey, List<String> tags) {
        List<ConfluencePage> pages = new ArrayList<>();
        String start = "0";
        int limit = 50;
        
        try {
            while (true) {
                String url = String.format("%s/rest/api/content?spaceKey=%s&limit=%d&start=%s&expand=body.storage,version,metadata.labels",
                        baseUrl, spaceKey, limit, start);
                
                Request request = new Request.Builder()
                        .url(url)
                        .header("Authorization", "Bearer " + authToken)
                        .header("Accept", "application/json")
                        .get()
                        .build();
                
                try (Response response = httpClient.newCall(request).execute()) {
                    if (!response.isSuccessful()) {
                        logger.error("Failed to fetch pages: {}", response);
                        break;
                    }
                    
                    JsonObject jsonResponse = gson.fromJson(response.body().string(), JsonObject.class);
                    JsonArray results = jsonResponse.getAsJsonArray("results");
                    
                    if (results == null || results.size() == 0) {
                        break;
                    }
                    
                    for (JsonElement element : results) {
                        JsonObject pageObj = element.getAsJsonObject();
                        ConfluencePage page = parsePage(pageObj);
                        
                        // Filter by tags if provided
                        if (tags == null || tags.isEmpty() || hasAnyTag(page, tags)) {
                            pages.add(page);
                        }
                    }
                    
                    // Check if there are more pages
                    JsonElement links = jsonResponse.get("_links");
                    if (links != null && links.getAsJsonObject().has("next")) {
                        start = String.valueOf(Integer.parseInt(start) + limit);
                    } else {
                        break;
                    }
                }
            }
        } catch (IOException e) {
            logger.error("Error fetching RCA pages", e);
        }
        
        return pages;
    }
    
    @Override
    public ConfluencePage fetchPageById(String pageId) {
        try {
            String url = String.format("%s/rest/api/content/%s?expand=body.storage,version,metadata.labels",
                    baseUrl, pageId);
            
            Request request = new Request.Builder()
                    .url(url)
                    .header("Authorization", "Bearer " + authToken)
                    .header("Accept", "application/json")
                    .get()
                    .build();
            
            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    logger.error("Failed to fetch page {}: {}", pageId, response);
                    return null;
                }
                
                JsonObject pageObj = gson.fromJson(response.body().string(), JsonObject.class);
                return parsePage(pageObj);
            }
        } catch (IOException e) {
            logger.error("Error fetching page {}", pageId, e);
            return null;
        }
    }
    
    @Override
    public List<ConfluencePage> fetchModifiedPagesSince(LocalDateTime lastSync, List<String> spaceKeys, List<String> tags) {
        List<ConfluencePage> pages = new ArrayList<>();
        
        for (String spaceKey : spaceKeys) {
            List<ConfluencePage> spacePages = fetchRCAPages(spaceKey, tags);
            for (ConfluencePage page : spacePages) {
                if (page.getLastModified().isAfter(lastSync)) {
                    pages.add(page);
                }
            }
        }
        
        return pages;
    }
    
    @Override
    public boolean authenticate(String token) {
        try {
            String url = baseUrl + "/rest/api/user/current";
            Request request = new Request.Builder()
                    .url(url)
                    .header("Authorization", "Bearer " + token)
                    .get()
                    .build();
            
            try (Response response = httpClient.newCall(request).execute()) {
                return response.isSuccessful();
            }
        } catch (IOException e) {
            logger.error("Authentication failed", e);
            return false;
        }
    }
    
    @Override
    public boolean hasUserAccess(String userId, String spaceKey) {
        // TODO: Implement actual permission check via Confluence API
        // For now, return true (assume access)
        return true;
    }
    
    private ConfluencePage parsePage(JsonObject pageObj) {
        String id = pageObj.get("id").getAsString();
        String title = pageObj.get("title").getAsString();
        JsonObject space = pageObj.getAsJsonObject("space");
        String spaceKey = space != null ? space.get("key").getAsString() : "";
        
        String url = baseUrl + pageObj.get("_links").getAsJsonObject().get("webui").getAsString();
        
        String body = "";
        if (pageObj.has("body") && pageObj.getAsJsonObject("body").has("storage")) {
            body = pageObj.getAsJsonObject("body")
                    .getAsJsonObject("storage")
                    .get("value").getAsString();
        }
        
        LocalDateTime lastModified = null;
        if (pageObj.has("version")) {
            String dateStr = pageObj.getAsJsonObject("version").get("when").getAsString();
            lastModified = LocalDateTime.parse(dateStr.substring(0, 19), 
                    DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"));
        }
        
        List<String> labels = new ArrayList<>();
        if (pageObj.has("metadata") && pageObj.getAsJsonObject("metadata").has("labels")) {
            JsonArray labelsArray = pageObj.getAsJsonObject("metadata")
                    .getAsJsonObject("labels")
                    .getAsJsonArray("results");
            for (JsonElement label : labelsArray) {
                labels.add(label.getAsJsonObject().get("name").getAsString());
            }
        }
        
        ConfluencePage page = new ConfluencePage();
        page.setId(id);
        page.setTitle(title);
        page.setSpaceKey(spaceKey);
        page.setUrl(url);
        page.setBody(body);
        page.setLastModified(lastModified != null ? lastModified : LocalDateTime.now());
        page.setLabels(labels);
        page.setStatus("current");
        return page;
    }
    
    private boolean hasAnyTag(ConfluencePage page, List<String> tags) {
        if (page.getLabels() == null) {
            return false;
        }
        return page.getLabels().stream().anyMatch(tags::contains);
    }
}

