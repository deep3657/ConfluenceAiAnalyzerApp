package com.confluenceai.analyzer.dto;

import java.util.List;

public class SearchResponse {
    private String query;
    private List<SearchResult> results;
    private Summary summary;
    private Long executionTimeMs;
    
    public SearchResponse() {}
    
    public String getQuery() { return query; }
    public void setQuery(String query) { this.query = query; }
    public List<SearchResult> getResults() { return results; }
    public void setResults(List<SearchResult> results) { this.results = results; }
    public Summary getSummary() { return summary; }
    public void setSummary(Summary summary) { this.summary = summary; }
    public Long getExecutionTimeMs() { return executionTimeMs; }
    public void setExecutionTimeMs(Long executionTimeMs) { this.executionTimeMs = executionTimeMs; }
    
    public static class Summary {
        private String suggestedRootCause;
        private String confidence; // High, Medium, Low
        private Integer similarIncidents;
        
        public Summary() {}
        
        public String getSuggestedRootCause() { return suggestedRootCause; }
        public void setSuggestedRootCause(String suggestedRootCause) { this.suggestedRootCause = suggestedRootCause; }
        public String getConfidence() { return confidence; }
        public void setConfidence(String confidence) { this.confidence = confidence; }
        public Integer getSimilarIncidents() { return similarIncidents; }
        public void setSimilarIncidents(Integer similarIncidents) { this.similarIncidents = similarIncidents; }
    }
}
