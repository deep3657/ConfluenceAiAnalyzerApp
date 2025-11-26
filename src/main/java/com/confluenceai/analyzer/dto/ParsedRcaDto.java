package com.confluenceai.analyzer.dto;

import java.time.LocalDateTime;

public class ParsedRcaDto {
    private String pageId;
    private String symptoms;
    private String rootCause;
    private String resolution;
    private LocalDateTime incidentDate;
    
    public ParsedRcaDto() {}
    
    public String getPageId() { return pageId; }
    public void setPageId(String pageId) { this.pageId = pageId; }
    public String getSymptoms() { return symptoms; }
    public void setSymptoms(String symptoms) { this.symptoms = symptoms; }
    public String getRootCause() { return rootCause; }
    public void setRootCause(String rootCause) { this.rootCause = rootCause; }
    public String getResolution() { return resolution; }
    public void setResolution(String resolution) { this.resolution = resolution; }
    public LocalDateTime getIncidentDate() { return incidentDate; }
    public void setIncidentDate(LocalDateTime incidentDate) { this.incidentDate = incidentDate; }
}
