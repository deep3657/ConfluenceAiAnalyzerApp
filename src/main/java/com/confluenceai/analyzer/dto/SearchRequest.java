package com.confluenceai.analyzer.dto;

import java.time.LocalDate;
import java.util.List;

public class SearchRequest {
    private String query;
    private Integer topK;
    private FilterBy filterBy;
    
    public SearchRequest() {}
    
    public String getQuery() { return query; }
    public void setQuery(String query) { this.query = query; }
    public Integer getTopK() { return topK; }
    public void setTopK(Integer topK) { this.topK = topK; }
    public FilterBy getFilterBy() { return filterBy; }
    public void setFilterBy(FilterBy filterBy) { this.filterBy = filterBy; }
    
    public static class FilterBy {
        private List<String> spaceKeys;
        private DateRange dateRange;
        
        public FilterBy() {}
        
        public List<String> getSpaceKeys() { return spaceKeys; }
        public void setSpaceKeys(List<String> spaceKeys) { this.spaceKeys = spaceKeys; }
        public DateRange getDateRange() { return dateRange; }
        public void setDateRange(DateRange dateRange) { this.dateRange = dateRange; }
    }
    
    public static class DateRange {
        private LocalDate from;
        private LocalDate to;
        
        public DateRange() {}
        
        public LocalDate getFrom() { return from; }
        public void setFrom(LocalDate from) { this.from = from; }
        public LocalDate getTo() { return to; }
        public void setTo(LocalDate to) { this.to = to; }
    }
}
