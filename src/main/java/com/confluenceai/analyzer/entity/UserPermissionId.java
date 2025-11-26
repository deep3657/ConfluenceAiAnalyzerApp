package com.confluenceai.analyzer.entity;

import java.io.Serializable;
import java.util.Objects;

public class UserPermissionId implements Serializable {
    private String userId;
    private String spaceKey;
    
    public UserPermissionId() {}
    
    public UserPermissionId(String userId, String spaceKey) {
        this.userId = userId;
        this.spaceKey = spaceKey;
    }
    
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getSpaceKey() { return spaceKey; }
    public void setSpaceKey(String spaceKey) { this.spaceKey = spaceKey; }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserPermissionId that = (UserPermissionId) o;
        return Objects.equals(userId, that.userId) && Objects.equals(spaceKey, that.spaceKey);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(userId, spaceKey);
    }
}
