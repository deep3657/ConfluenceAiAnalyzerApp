package com.confluenceai.analyzer.dto;

public class SearchResult {
    private String pageId;
    private String title;
    private String content;
    private String confluenceUrl;
    private Double similarityScore;
    private String chunkType; // SYMPTOMS or ROOT_CAUSE
    private ParsedRcaDto fullRCA;
    
    public SearchResult() {}
    
    public String getPageId() { return pageId; }
    public void setPageId(String pageId) { this.pageId = pageId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getConfluenceUrl() { return confluenceUrl; }
    public void setConfluenceUrl(String confluenceUrl) { this.confluenceUrl = confluenceUrl; }
    public Double getSimilarityScore() { return similarityScore; }
    public void setSimilarityScore(Double similarityScore) { this.similarityScore = similarityScore; }
    public String getChunkType() { return chunkType; }
    public void setChunkType(String chunkType) { this.chunkType = chunkType; }
    public ParsedRcaDto getFullRCA() { return fullRCA; }
    public void setFullRCA(ParsedRcaDto fullRCA) { this.fullRCA = fullRCA; }
}
