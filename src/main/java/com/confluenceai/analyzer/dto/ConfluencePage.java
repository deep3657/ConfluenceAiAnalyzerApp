package com.confluenceai.analyzer.dto;

import java.time.LocalDateTime;
import java.util.List;

public class ConfluencePage {
    private String id;
    private String title;
    private String spaceKey;
    private String url;
    private String body; // HTML content
    private LocalDateTime lastModified;
    private List<String> labels;
    private String status; // current, archived, etc.
    
    public ConfluencePage() {}
    
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getSpaceKey() { return spaceKey; }
    public void setSpaceKey(String spaceKey) { this.spaceKey = spaceKey; }
    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }
    public LocalDateTime getLastModified() { return lastModified; }
    public void setLastModified(LocalDateTime lastModified) { this.lastModified = lastModified; }
    public List<String> getLabels() { return labels; }
    public void setLabels(List<String> labels) { this.labels = labels; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
