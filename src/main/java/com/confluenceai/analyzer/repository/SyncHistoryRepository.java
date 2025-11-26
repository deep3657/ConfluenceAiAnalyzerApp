package com.confluenceai.analyzer.repository;

import com.confluenceai.analyzer.entity.SyncHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SyncHistoryRepository extends JpaRepository<SyncHistory, UUID> {
    
    List<SyncHistory> findByStatus(String status);
    
    Optional<SyncHistory> findFirstByOrderByStartedAtDesc();
    
    List<SyncHistory> findBySyncTypeOrderByStartedAtDesc(String syncType);
}
