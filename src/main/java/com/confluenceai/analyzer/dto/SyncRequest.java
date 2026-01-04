package com.confluenceai.analyzer.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import java.util.ArrayList;
import java.util.List;

public class SyncRequest {
    @JsonAlias({"spaces", "spaceKeys"})
    private List<String> spaceKeys;
    private String syncType; // FULL, INCREMENTAL
    private List<String> tags;
    private Integer limit; // Optional: limit number of pages to process
    
    public SyncRequest() {}
    
    public List<String> getSpaceKeys() { 
        return spaceKeys != null ? spaceKeys : new ArrayList<>(); 
    }
    public void setSpaceKeys(List<String> spaceKeys) { this.spaceKeys = spaceKeys; }
    
    // Alias setter for frontend compatibility
    public void setSpaces(List<String> spaces) { this.spaceKeys = spaces; }
    
    public String getSyncType() { return syncType; }
    public void setSyncType(String syncType) { this.syncType = syncType; }
    public List<String> getTags() { 
        return tags != null ? tags : new ArrayList<>(); 
    }
    public void setTags(List<String> tags) { this.tags = tags; }
    
    public Integer getLimit() { return limit; }
    public void setLimit(Integer limit) { this.limit = limit; }
}
