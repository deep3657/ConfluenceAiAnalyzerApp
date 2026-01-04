package com.confluenceai.analyzer.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public class SyncResponse {
    private UUID syncId;
    private String status; // RUNNING, COMPLETED, FAILED
    private String message;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private LocalDateTime estimatedCompletionTime;
    private Integer pagesFetched;
    private Integer pagesProcessed;
    private Integer pagesFailed;
    
    public SyncResponse() {}
    
    public UUID getSyncId() { return syncId; }
    public void setSyncId(UUID syncId) { this.syncId = syncId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public LocalDateTime getStartedAt() { return startedAt; }
    public void setStartedAt(LocalDateTime startedAt) { this.startedAt = startedAt; }
    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }
    public LocalDateTime getEstimatedCompletionTime() { return estimatedCompletionTime; }
    public void setEstimatedCompletionTime(LocalDateTime estimatedCompletionTime) { this.estimatedCompletionTime = estimatedCompletionTime; }
    public Integer getPagesFetched() { return pagesFetched; }
    public void setPagesFetched(Integer pagesFetched) { this.pagesFetched = pagesFetched; }
    public Integer getPagesProcessed() { return pagesProcessed; }
    public void setPagesProcessed(Integer pagesProcessed) { this.pagesProcessed = pagesProcessed; }
    public Integer getPagesFailed() { return pagesFailed; }
    public void setPagesFailed(Integer pagesFailed) { this.pagesFailed = pagesFailed; }
}
