package com.confluenceai.analyzer.dto;

import java.util.List;

public class SyncRequest {
    private List<String> spaceKeys;
    private String syncType; // FULL, INCREMENTAL
    private List<String> tags;
    
    public SyncRequest() {}
    
    public List<String> getSpaceKeys() { return spaceKeys; }
    public void setSpaceKeys(List<String> spaceKeys) { this.spaceKeys = spaceKeys; }
    public String getSyncType() { return syncType; }
    public void setSyncType(String syncType) { this.syncType = syncType; }
    public List<String> getTags() { return tags; }
    public void setTags(List<String> tags) { this.tags = tags; }
}
