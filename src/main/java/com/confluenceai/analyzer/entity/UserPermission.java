package com.confluenceai.analyzer.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_permissions")
@IdClass(UserPermissionId.class)
public class UserPermission {
    
    @Id
    @Column(name = "user_id", length = 255)
    private String userId;
    
    @Id
    @Column(name = "space_key", length = 255)
    private String spaceKey;
    
    @Column(name = "has_access", nullable = false)
    private Boolean hasAccess = false;
    
    @Column(name = "cached_at")
    private LocalDateTime cachedAt;
    
    @Column(name = "expires_at")
    private LocalDateTime expiresAt;
    
    public UserPermission() {}
    
    @PrePersist
    protected void onCreate() {
        if (cachedAt == null) {
            cachedAt = LocalDateTime.now();
        }
    }
    
    // Getters and Setters
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getSpaceKey() { return spaceKey; }
    public void setSpaceKey(String spaceKey) { this.spaceKey = spaceKey; }
    public Boolean getHasAccess() { return hasAccess; }
    public void setHasAccess(Boolean hasAccess) { this.hasAccess = hasAccess; }
    public LocalDateTime getCachedAt() { return cachedAt; }
    public void setCachedAt(LocalDateTime cachedAt) { this.cachedAt = cachedAt; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }
}
