package com.confluenceai.analyzer.repository;

import com.confluenceai.analyzer.entity.ParsedRca;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ParsedRcaRepository extends JpaRepository<ParsedRca, UUID> {
    
    Optional<ParsedRca> findByPageId(String pageId);
    
    void deleteByPageId(String pageId);
}
