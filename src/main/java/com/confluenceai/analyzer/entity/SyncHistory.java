package com.confluenceai.analyzer.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "sync_history")
public class SyncHistory {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    private UUID id;
    
    @Column(name = "sync_type", nullable = false, length = 50)
    private String syncType; // FULL, INCREMENTAL
    
    @Column(name = "spaces_synced", columnDefinition = "TEXT[]")
    private String[] spacesSynced;
    
    @Column(name = "pages_fetched")
    private Integer pagesFetched = 0;
    
    @Column(name = "pages_processed")
    private Integer pagesProcessed = 0;
    
    @Column(name = "pages_failed")
    private Integer pagesFailed = 0;
    
    @Column(name = "started_at")
    private LocalDateTime startedAt;
    
    @Column(name = "completed_at")
    private LocalDateTime completedAt;
    
    @Column(name = "status", length = 50)
    private String status = "RUNNING"; // RUNNING, COMPLETED, FAILED
    
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    public SyncHistory() {}
    
    @PrePersist
    protected void onCreate() {
        if (startedAt == null) {
            startedAt = LocalDateTime.now();
        }
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
    
    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getSyncType() { return syncType; }
    public void setSyncType(String syncType) { this.syncType = syncType; }
    public String[] getSpacesSynced() { return spacesSynced; }
    public void setSpacesSynced(String[] spacesSynced) { this.spacesSynced = spacesSynced; }
    public Integer getPagesFetched() { return pagesFetched; }
    public void setPagesFetched(Integer pagesFetched) { this.pagesFetched = pagesFetched; }
    public Integer getPagesProcessed() { return pagesProcessed; }
    public void setPagesProcessed(Integer pagesProcessed) { this.pagesProcessed = pagesProcessed; }
    public Integer getPagesFailed() { return pagesFailed; }
    public void setPagesFailed(Integer pagesFailed) { this.pagesFailed = pagesFailed; }
    public LocalDateTime getStartedAt() { return startedAt; }
    public void setStartedAt(LocalDateTime startedAt) { this.startedAt = startedAt; }
    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
