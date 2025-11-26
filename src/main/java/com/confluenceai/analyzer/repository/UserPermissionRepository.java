package com.confluenceai.analyzer.repository;

import com.confluenceai.analyzer.entity.UserPermission;
import com.confluenceai.analyzer.entity.UserPermissionId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserPermissionRepository extends JpaRepository<UserPermission, UserPermissionId> {
    
    Optional<UserPermission> findByUserIdAndSpaceKey(String userId, String spaceKey);
    
    List<UserPermission> findByUserId(String userId);
    
    @Modifying
    @Query("DELETE FROM UserPermission up WHERE up.expiresAt IS NOT NULL AND up.expiresAt < :now")
    void deleteExpiredPermissions(@Param("now") LocalDateTime now);
}
