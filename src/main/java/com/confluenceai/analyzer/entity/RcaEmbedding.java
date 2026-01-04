package com.confluenceai.analyzer.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "rca_embeddings", 
       uniqueConstraints = @UniqueConstraint(columnNames = {"page_id", "chunk_index", "chunk_type"}))
public class RcaEmbedding {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    private UUID id;
    
    @Column(name = "page_id", nullable = false, length = 255)
    private String pageId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "page_id", insertable = false, updatable = false)
    private RcaPage rcaPage;
    
    @Column(name = "chunk_index", nullable = false)
    private Integer chunkIndex;
    
    @Column(name = "chunk_type", nullable = false, length = 50)
    private String chunkType; // SYMPTOMS or ROOT_CAUSE
    
    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;
    
    @Column(name = "embedding", columnDefinition = "vector(768)")
    private String embedding; // Stored as string, converted to/from vector in repository
    
    @Column(name = "metadata", columnDefinition = "JSONB")
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> metadata;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    public RcaEmbedding() {}
    
    // Helper method to convert List<Float> to vector string format
    public void setEmbeddingVector(List<Float> vector) {
        if (vector != null && !vector.isEmpty()) {
            this.embedding = "[" + vector.stream()
                    .map(f -> String.format("%.6f", f))
                    .reduce((a, b) -> a + "," + b)
                    .orElse("") + "]";
        }
    }
    
    // Helper method to convert vector string to List<Float>
    public List<Float> getEmbeddingVector() {
        if (embedding == null || embedding.isEmpty()) {
            return List.of();
        }
        String clean = embedding.replace("[", "").replace("]", "");
        return java.util.Arrays.stream(clean.split(","))
                .map(String::trim)
                .map(Float::parseFloat)
                .toList();
    }
    
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
    public Integer getChunkIndex() { return chunkIndex; }
    public void setChunkIndex(Integer chunkIndex) { this.chunkIndex = chunkIndex; }
    public String getChunkType() { return chunkType; }
    public void setChunkType(String chunkType) { this.chunkType = chunkType; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getEmbedding() { return embedding; }
    public void setEmbedding(String embedding) { this.embedding = embedding; }
    public Map<String, Object> getMetadata() { return metadata; }
    public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
