package com.confluenceai.analyzer.repository;

import com.confluenceai.analyzer.entity.RcaEmbedding;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Repository
public interface RcaEmbeddingRepository extends JpaRepository<RcaEmbedding, UUID> {
    
    List<RcaEmbedding> findByPageId(String pageId);
    
    List<RcaEmbedding> findByPageIdAndChunkType(String pageId, String chunkType);
    
    @Modifying
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
    void deleteByPageId(String pageId);
    
    // Native insert with proper vector casting
    @Modifying
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
    @Query(value = """
        INSERT INTO rca_embeddings (id, page_id, chunk_index, chunk_type, content, embedding, created_at, updated_at)
        VALUES (:id, :pageId, :chunkIndex, :chunkType, :content, CAST(:embedding AS vector), :createdAt, :updatedAt)
        """, nativeQuery = true)
    void insertWithVector(
        @Param("id") UUID id,
        @Param("pageId") String pageId,
        @Param("chunkIndex") Integer chunkIndex,
        @Param("chunkType") String chunkType,
        @Param("content") String content,
        @Param("embedding") String embedding,
        @Param("createdAt") java.time.LocalDateTime createdAt,
        @Param("updatedAt") java.time.LocalDateTime updatedAt
    );
    
    // Vector similarity search using cosine distance
    // Note: This uses native SQL because JPA doesn't support vector operations directly
    @Query(value = """
        SELECT e.*, 1 - (e.embedding <=> CAST(:queryVector AS vector)) AS similarity
        FROM rca_embeddings e
        WHERE e.embedding <=> CAST(:queryVector AS vector) < :maxDistance
        ORDER BY e.embedding <=> CAST(:queryVector AS vector)
        LIMIT :limit
        """, nativeQuery = true)
    List<Object[]> findSimilarEmbeddings(
        @Param("queryVector") String queryVector,
        @Param("maxDistance") double maxDistance,
        @Param("limit") int limit
    );
    
    // Vector similarity search with metadata filtering
    @Query(value = """
        SELECT e.*, 1 - (e.embedding <=> CAST(:queryVector AS vector)) AS similarity
        FROM rca_embeddings e
        WHERE e.embedding <=> CAST(:queryVector AS vector) < :maxDistance
        AND e.chunk_type = :chunkType
        AND (:spaceKey IS NULL OR e.page_id IN (
            SELECT page_id FROM rca_pages WHERE space_key = :spaceKey
        ))
        ORDER BY e.embedding <=> CAST(:queryVector AS vector)
        LIMIT :limit
        """, nativeQuery = true)
    List<Object[]> findSimilarEmbeddingsWithFilters(
        @Param("queryVector") String queryVector,
        @Param("maxDistance") double maxDistance,
        @Param("chunkType") String chunkType,
        @Param("spaceKey") String spaceKey,
        @Param("limit") int limit
    );
}
