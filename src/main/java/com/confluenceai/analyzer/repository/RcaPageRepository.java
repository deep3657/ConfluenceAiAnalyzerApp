package com.confluenceai.analyzer.repository;

import com.confluenceai.analyzer.entity.RcaPage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface RcaPageRepository extends JpaRepository<RcaPage, String> {
    
    List<RcaPage> findBySpaceKey(String spaceKey);
    
    List<RcaPage> findByStatus(String status);
    
    List<RcaPage> findByLastModifiedAfter(LocalDateTime lastModified);
    
    @Query("SELECT rp FROM RcaPage rp WHERE rp.spaceKey IN :spaceKeys AND rp.status = :status")
    List<RcaPage> findBySpaceKeysAndStatus(@Param("spaceKeys") List<String> spaceKeys, @Param("status") String status);
    
    @Query(value = "SELECT * FROM rca_pages WHERE :tag = ANY(tags)", nativeQuery = true)
    List<RcaPage> findByTag(@Param("tag") String tag);
    
    Optional<RcaPage> findByPageId(String pageId);
}
