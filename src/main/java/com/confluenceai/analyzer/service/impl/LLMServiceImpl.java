package com.confluenceai.analyzer.service.impl;

import com.confluenceai.analyzer.dto.SearchResult;
import com.confluenceai.analyzer.service.LLMService;
import com.theokanning.openai.service.OpenAiService;
import com.theokanning.openai.service.OpenAiService;
import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.completion.chat.ChatMessageRole;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class LLMServiceImpl implements LLMService {
    
    private static final Logger logger = LoggerFactory.getLogger(LLMServiceImpl.class);
    
    private final OpenAiService openAiService;
    private final String model;
    private final double temperature;
    private final int maxTokens;
    
    public LLMServiceImpl(
            @Value("${llm.model}") String model,
            @Value("${llm.temperature:0.3}") double temperature,
            @Value("${llm.max-tokens:1000}") int maxTokens,
            @Value("${llm.api-key}") String apiKey) {
        this.model = model;
        this.temperature = temperature;
        this.maxTokens = maxTokens;
        this.openAiService = new OpenAiService(apiKey);
    }
    
    @Override
    public String generateSummary(String userQuery, List<SearchResult> results) {
        if (results == null || results.isEmpty()) {
            return "No similar historical incidents found.";
        }
        
        String context = buildContextFromResults(results);
        String prompt = buildPrompt(userQuery, context);
        
        try {
            List<ChatMessage> messages = new ArrayList<>();
            messages.add(new ChatMessage(ChatMessageRole.SYSTEM.value(), 
                    "You are an expert SRE analyzing incident reports. Provide concise, accurate analysis based on historical data."));
            messages.add(new ChatMessage(ChatMessageRole.USER.value(), prompt));
            
            ChatCompletionRequest request = ChatCompletionRequest.builder()
                    .model(model)
                    .messages(messages)
                    .temperature(temperature)
                    .maxTokens(maxTokens)
                    .build();
            
            String response = openAiService.createChatCompletion(request)
                    .getChoices().get(0).getMessage().getContent();
            
            return response;
        } catch (Exception e) {
            logger.error("Error generating LLM summary", e);
            return "Error generating summary. Please try again.";
        }
    }
    
    @Override
    public String synthesizeRootCause(List<SearchResult> results) {
        if (results == null || results.isEmpty()) {
            return "No similar historical incidents found.";
        }
        
        String context = results.stream()
                .map(r -> String.format("RCA: %s\nRoot Cause: %s\n", 
                        r.getTitle(), 
                        r.getFullRCA() != null ? r.getFullRCA().getRootCause() : ""))
                .collect(Collectors.joining("\n"));
        
        String prompt = String.format("""
                Based on the following historical Root Cause Analysis documents, suggest the most likely root cause for a similar incident.
                
                Historical RCAs:
                %s
                
                Provide a concise root cause analysis. If no clear pattern emerges, state: "No similar historical incidents found."
                """, context);
        
        try {
            List<ChatMessage> messages = new ArrayList<>();
            messages.add(new ChatMessage(ChatMessageRole.SYSTEM.value(), 
                    "You are an expert SRE. Analyze root causes from historical incidents."));
            messages.add(new ChatMessage(ChatMessageRole.USER.value(), prompt));
            
            ChatCompletionRequest request = ChatCompletionRequest.builder()
                    .model(model)
                    .messages(messages)
                    .temperature(temperature)
                    .maxTokens(maxTokens)
                    .build();
            
            return openAiService.createChatCompletion(request)
                    .getChoices().get(0).getMessage().getContent();
        } catch (Exception e) {
            logger.error("Error synthesizing root cause", e);
            return "Error synthesizing root cause. Please try again.";
        }
    }
    
    private String buildContextFromResults(List<SearchResult> results) {
        return results.stream()
                .map(r -> String.format("""
                        - RCA: %s (Similarity: %.2f)
                          Symptoms: %s
                          Root Cause: %s
                          Resolution: %s
                          Link: %s
                        """,
                        r.getTitle(),
                        r.getSimilarityScore(),
                        r.getFullRCA() != null ? r.getFullRCA().getSymptoms() : "",
                        r.getFullRCA() != null ? r.getFullRCA().getRootCause() : "",
                        r.getFullRCA() != null ? r.getFullRCA().getResolution() : "",
                        r.getConfluenceUrl()))
                .collect(Collectors.joining("\n"));
    }
    
    private String buildPrompt(String userQuery, String context) {
        return String.format("""
                You are an expert SRE analyzing incident reports. Based on the following historical Root Cause Analysis documents, suggest a potential root cause for the current issue.
                
                Current Issue:
                %s
                
                Historical RCAs:
                %s
                
                Instructions:
                1. Analyze the symptoms and root causes from historical RCAs
                2. Identify patterns and similarities
                3. Suggest the most likely root cause
                4. If no relevant historical data exists, state: "No similar historical incidents found."
                5. Always cite the source RCA documents
                
                Format your response as:
                - Suggested Root Cause: [your analysis]
                - Confidence: [High/Medium/Low]
                - Similar Historical Incidents: [list with links]
                """, userQuery, context);
    }
}

