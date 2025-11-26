package com.confluenceai.analyzer.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {
    
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("RCA Insight Engine API")
                        .version("1.0.0")
                        .description("""
                                API for RCA Insight Engine (Project "Recall")
                                
                                This API provides endpoints for:
                                - Searching historical RCA documents using semantic similarity
                                - Ingesting and syncing Confluence RCA pages
                                - Managing RCA data and metadata
                                
                                The system uses RAG (Retrieval-Augmented Generation) to provide
                                intelligent root cause suggestions based on historical incidents.
                                """)
                        .contact(new Contact()
                                .name("RCA Insight Engine Team")
                                .email("support@example.com"))
                        .license(new License()
                                .name("Proprietary")
                                .url("https://example.com/license")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:8080")
                                .description("Local Development Server"),
                        new Server()
                                .url("https://api.example.com")
                                .description("Production Server")
                ));
    }
}

