package com.confluenceai.analyzer.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "parsed_rca")
public class ParsedRca {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    private UUID id;
    
    @Column(name = "page_id", nullable = false, length = 255)
    private String pageId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "page_id", insertable = false, updatable = false)
    private RcaPage rcaPage;
    
    @Column(name = "symptoms", columnDefinition = "TEXT")
    private String symptoms;
    
    @Column(name = "root_cause", columnDefinition = "TEXT")
    private String rootCause;
    
    @Column(name = "resolution", columnDefinition = "TEXT")
    private String resolution;
    
    @Column(name = "incident_date")
    private LocalDateTime incidentDate;
    
    @Column(name = "parsed_content", columnDefinition = "JSONB")
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> parsedContent;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    public ParsedRca() {}
    
    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (updatedAt == null) {
            updatedAt = LocalDateTime.now();
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getPageId() { return pageId; }
    public void setPageId(String pageId) { this.pageId = pageId; }
    public RcaPage getRcaPage() { return rcaPage; }
    public void setRcaPage(RcaPage rcaPage) { this.rcaPage = rcaPage; }
    public String getSymptoms() { return symptoms; }
    public void setSymptoms(String symptoms) { this.symptoms = symptoms; }
    public String getRootCause() { return rootCause; }
    public void setRootCause(String rootCause) { this.rootCause = rootCause; }
    public String getResolution() { return resolution; }
    public void setResolution(String resolution) { this.resolution = resolution; }
    public LocalDateTime getIncidentDate() { return incidentDate; }
    public void setIncidentDate(LocalDateTime incidentDate) { this.incidentDate = incidentDate; }
    public Map<String, Object> getParsedContent() { return parsedContent; }
    public void setParsedContent(Map<String, Object> parsedContent) { this.parsedContent = parsedContent; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
