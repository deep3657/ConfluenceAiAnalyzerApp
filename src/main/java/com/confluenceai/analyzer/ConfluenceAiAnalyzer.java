package com.confluenceai.analyzer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main application class for AI-based analysis of Confluence content.
 */
@SpringBootApplication
public class ConfluenceAiAnalyzer {
    
    private static final Logger logger = LoggerFactory.getLogger(ConfluenceAiAnalyzer.class);
    
    public static void main(String[] args) {
        logger.info("Confluence AI Analyzer starting...");
        SpringApplication.run(ConfluenceAiAnalyzer.class, args);
        logger.info("Confluence AI Analyzer application started successfully.");
    }
}

