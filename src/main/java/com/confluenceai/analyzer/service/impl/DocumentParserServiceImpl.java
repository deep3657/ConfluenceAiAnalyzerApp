package com.confluenceai.analyzer.service.impl;

import com.confluenceai.analyzer.dto.ConfluencePage;
import com.confluenceai.analyzer.dto.ParsedRcaDto;
import com.confluenceai.analyzer.service.DocumentParserService;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Service
public class DocumentParserServiceImpl implements DocumentParserService {
    
    private static final Logger logger = LoggerFactory.getLogger(DocumentParserServiceImpl.class);
    
    // Keywords to identify sections
    private static final Pattern SYMPTOMS_PATTERNS = Pattern.compile(
            "(?i)(symptoms|impact|alerts? fired|user reports?|what happened|incident description)");
    private static final Pattern ROOT_CAUSE_PATTERNS = Pattern.compile(
            "(?i)(root cause|technical fault|why|the fix|resolution|what was the problem)");
    private static final Pattern RESOLUTION_PATTERNS = Pattern.compile(
            "(?i)(resolution|fix|solution|action taken|remediation)");
    
    @Override
    public ParsedRcaDto parseDocument(ConfluencePage page) {
        String rawContent = page.getBody();
        String content = cleanHtml(rawContent);
        Document doc = Jsoup.parse(rawContent);
        
        String symptoms = extractSymptoms(rawContent).stream()
                .reduce((a, b) -> a + "\n" + b)
                .orElse("");
        
        String rootCause = extractRootCause(rawContent);
        String resolution = extractResolution(rawContent);
        LocalDateTime incidentDate = extractIncidentDate(doc);
        
        // If no specific sections found, use the full content
        // This ensures we always have something to embed
        if (symptoms.isEmpty() && rootCause.isEmpty()) {
            logger.debug("No specific sections found for page {}, using full content", page.getId());
            // Use full cleaned content as "symptoms" for embedding
            symptoms = content;
        }
        
        ParsedRcaDto dto = new ParsedRcaDto();
        dto.setPageId(page.getId());
        dto.setSymptoms(symptoms);
        dto.setRootCause(rootCause);
        dto.setResolution(resolution);
        dto.setIncidentDate(incidentDate);
        return dto;
    }
    
    @Override
    public List<String> extractSymptoms(String content) {
        List<String> symptoms = new ArrayList<>();
        Document doc = Jsoup.parse(content);
        
        // Look for headers matching symptoms patterns
        Elements headers = doc.select("h1, h2, h3, h4, h5, h6");
        for (Element header : headers) {
            String headerText = header.text();
            if (SYMPTOMS_PATTERNS.matcher(headerText).find()) {
                // Get content after this header until next header
                Element next = header.nextElementSibling();
                StringBuilder symptomText = new StringBuilder();
                while (next != null && !next.tagName().matches("h[1-6]")) {
                    symptomText.append(next.text()).append("\n");
                    next = next.nextElementSibling();
                }
                if (!symptomText.toString().trim().isEmpty()) {
                    symptoms.add(symptomText.toString().trim());
                }
            }
        }
        
        return symptoms;
    }
    
    @Override
    public String extractRootCause(String content) {
        Document doc = Jsoup.parse(content);
        Elements headers = doc.select("h1, h2, h3, h4, h5, h6");
        
        for (Element header : headers) {
            String headerText = header.text();
            if (ROOT_CAUSE_PATTERNS.matcher(headerText).find()) {
                Element next = header.nextElementSibling();
                StringBuilder rootCauseText = new StringBuilder();
                while (next != null && !next.tagName().matches("h[1-6]")) {
                    rootCauseText.append(next.text()).append("\n");
                    next = next.nextElementSibling();
                }
                return rootCauseText.toString().trim();
            }
        }
        
        return "";
    }
    
    @Override
    public String extractResolution(String content) {
        Document doc = Jsoup.parse(content);
        Elements headers = doc.select("h1, h2, h3, h4, h5, h6");
        
        for (Element header : headers) {
            String headerText = header.text();
            if (RESOLUTION_PATTERNS.matcher(headerText).find()) {
                Element next = header.nextElementSibling();
                StringBuilder resolutionText = new StringBuilder();
                while (next != null && !next.tagName().matches("h[1-6]")) {
                    resolutionText.append(next.text()).append("\n");
                    next = next.nextElementSibling();
                }
                return resolutionText.toString().trim();
            }
        }
        
        return "";
    }
    
    @Override
    public List<String> chunkContent(String content, int chunkSize, int overlap) {
        List<String> chunks = new ArrayList<>();
        String cleanText = cleanHtml(content);
        
        // Simple character-based chunking (can be improved with semantic chunking)
        int start = 0;
        while (start < cleanText.length()) {
            int end = Math.min(start + chunkSize, cleanText.length());
            String chunk = cleanText.substring(start, end);
            chunks.add(chunk);
            start += (chunkSize - overlap);
        }
        
        return chunks;
    }
    
    private String cleanHtml(String html) {
        if (html == null) {
            return "";
        }
        Document doc = Jsoup.parse(html);
        // Remove scripts, styles, and navigation elements
        doc.select("script, style, nav, .navigation, .sidebar").remove();
        return doc.text();
    }
    
    private LocalDateTime extractIncidentDate(Document doc) {
        // Try to find date patterns in the document
        String text = doc.text();
        Pattern datePattern = Pattern.compile("\\d{4}-\\d{2}-\\d{2}");
        java.util.regex.Matcher matcher = datePattern.matcher(text);
        if (matcher.find()) {
            try {
                return LocalDateTime.parse(matcher.group() + "T00:00:00",
                        DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            } catch (Exception e) {
                logger.debug("Could not parse date: {}", matcher.group());
            }
        }
        return null;
    }
}

