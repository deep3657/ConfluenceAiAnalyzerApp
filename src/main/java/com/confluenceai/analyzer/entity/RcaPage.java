package com.confluenceai.analyzer.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "rca_pages")
public class RcaPage {
    
    @Id
    @Column(name = "page_id", length = 255)
    private String pageId;
    
    @Column(name = "space_key", nullable = false, length = 255)
    private String spaceKey;
    
    @Column(name = "title", nullable = false, length = 500)
    private String title;
    
    @Column(name = "url", nullable = false, columnDefinition = "TEXT")
    private String url;
    
    @Column(name = "tags", columnDefinition = "TEXT[]")
    private String[] tags;
    
    @Column(name = "last_modified", nullable = false)
    private LocalDateTime lastModified;
    
    @Column(name = "ingested_at")
    private LocalDateTime ingestedAt;
    
    @Column(name = "parsed_at")
    private LocalDateTime parsedAt;
    
    @Column(name = "embedding_generated_at")
    private LocalDateTime embeddingGeneratedAt;
    
    @Column(name = "status", length = 50)
    private String status = "PENDING"; // PENDING, PARSED, EMBEDDED, ERROR
    
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    public RcaPage() {}
    
    @PrePersist
    protected void onCreate() {
        if (ingestedAt == null) {
            ingestedAt = LocalDateTime.now();
        }
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
    
    // Getters and Setters
    public String getPageId() { return pageId; }
    public void setPageId(String pageId) { this.pageId = pageId; }
    public String getSpaceKey() { return spaceKey; }
    public void setSpaceKey(String spaceKey) { this.spaceKey = spaceKey; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
    public String[] getTags() { return tags; }
    public void setTags(String[] tags) { this.tags = tags; }
    public LocalDateTime getLastModified() { return lastModified; }
    public void setLastModified(LocalDateTime lastModified) { this.lastModified = lastModified; }
    public LocalDateTime getIngestedAt() { return ingestedAt; }
    public void setIngestedAt(LocalDateTime ingestedAt) { this.ingestedAt = ingestedAt; }
    public LocalDateTime getParsedAt() { return parsedAt; }
    public void setParsedAt(LocalDateTime parsedAt) { this.parsedAt = parsedAt; }
    public LocalDateTime getEmbeddingGeneratedAt() { return embeddingGeneratedAt; }
    public void setEmbeddingGeneratedAt(LocalDateTime embeddingGeneratedAt) { this.embeddingGeneratedAt = embeddingGeneratedAt; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
