# Design Document: RCA Insight Engine (Project "Recall")

## 1. Document Information

- **Project Name**: RCA Insight Engine (Project "Recall")
- **Version**: 1.0
- **Date**: November 2024
- **Status**: Design Phase
- **Technology Stack**: Java 25, Spring Boot 4.0, RAG Pipeline

## 2. Executive Summary

The RCA Insight Engine is a RAG (Retrieval-Augmented Generation) based system that ingests historical Root Cause Analysis documents from Atlassian Confluence, indexes them using vector embeddings, and provides intelligent suggestions for potential root causes of newly reported incidents based on semantic similarity.

### 2.1 Key Objectives
- Automate ingestion of RCA/Post-Mortem documents from Confluence
- Enable semantic search over historical incident data
- Provide AI-powered root cause suggestions with citations
- Reduce MTTR (Mean Time To Resolution) for repeat incidents

## 3. System Architecture

### 3.1 High-Level Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                         Client Layer                            │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐        │
│  │  Web UI      │  │  REST API    │  │  Jira Widget │        │
│  └──────────────┘  └──────────────┘  └──────────────┘        │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                    Application Layer (Spring Boot)               │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │  REST Controllers                                         │  │
│  │  - SearchController                                       │  │
│  │  - IngestionController                                    │  │
│  │  - HealthCheckController                                  │  │
│  └──────────────────────────────────────────────────────────┘  │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │  Service Layer                                            │  │
│  │  - ConfluenceService (Data Ingestion)                    │  │
│  │  - EmbeddingService (Vector Generation)                 │  │
│  │  - SearchService (Semantic Search)                       │  │
│  │  - LLMService (Response Generation)                      │  │
│  │  - DocumentParserService (RCA Parsing)                    │  │
│  └──────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘
                              │
        ┌─────────────────────┼─────────────────────┐
        ▼                     ▼                     ▼
┌──────────────┐    ┌──────────────┐    ┌──────────────┐
│  Confluence  │    │  Vector DB   │    │  LLM API      │
│  API         │    │  (PGVector/  │    │  (OpenAI/    │
│              │    │   Milvus)    │    │   Anthropic) │
└──────────────┘    └──────────────┘    └──────────────┘
                              │
                              ▼
                    ┌──────────────┐
                    │  PostgreSQL  │
                    │  (Metadata)  │
                    └──────────────┘
```

### 3.2 Component Architecture

#### 3.2.1 Data Ingestion Pipeline

```
Confluence API → Document Fetcher → Parser → Chunker → Embedder → Vector Store
     │                │                │         │          │           │
     │                │                │         │          │           │
     └────────────────┴────────────────┴────────┴──────────┴───────────┘
                    Metadata Store (PostgreSQL)
```

#### 3.2.2 Query Pipeline (RAG)

```
User Query → Embed Query → Vector Search → Retrieve Top-K → 
LLM Synthesis → Format Response → Return with Citations
```

## 4. Detailed Component Design

### 4.1 Confluence Integration Service

**Class**: `ConfluenceService`
**Responsibilities**:
- Authenticate with Confluence API (OAuth 2.0 / PAT)
- Fetch pages from specified spaces
- Filter pages by tags (rca, post-mortem)
- Incremental sync (only fetch modified pages)
- Handle pagination and rate limiting

**Key Methods**:
```java
public interface ConfluenceService {
    List<ConfluencePage> fetchRCAPages(String spaceKey, List<String> tags);
    ConfluencePage fetchPageById(String pageId);
    List<ConfluencePage> fetchModifiedPagesSince(LocalDateTime lastSync);
    boolean authenticate(String token);
}
```

**Configuration**:
- Confluence base URL
- Authentication token (PAT or OAuth)
- Space keys to monitor
- Tags to filter (rca, post-mortem)
- Sync schedule (cron expression)

### 4.2 Document Parser Service

**Class**: `DocumentParserService`
**Responsibilities**:
- Extract structured data from unstructured HTML/text
- Identify key sections: Symptoms, Root Cause, Resolution, Timeline
- Clean HTML tags, macros, navigation text
- Apply "5 Whys" extraction strategy
- Handle inconsistent RCA formats using LLM

**Key Methods**:
```java
public interface DocumentParserService {
    ParsedRCA parseDocument(ConfluencePage page);
    List<TextChunk> extractSymptoms(String content);
    String extractRootCause(String content);
    String extractResolution(String content);
    LocalDateTime extractTimeline(String content);
}
```

**Data Model**:
```java
public class ParsedRCA {
    private String pageId;
    private String title;
    private String symptoms;      // Input segment
    private String rootCause;      // Output segment
    private String resolution;
    private LocalDateTime incidentDate;
    private LocalDateTime lastModified;
    private String confluenceUrl;
    private Map<String, String> metadata;
}
```

### 4.3 Text Chunking Service

**Class**: `ChunkingService`
**Responsibilities**:
- Split documents into semantic chunks
- Preserve context (overlap between chunks)
- Optimize chunk size for embedding models
- Maintain chunk metadata (source, position)

**Strategy**:
- Chunk size: 500-1000 tokens
- Overlap: 100-200 tokens
- Separate chunks for Symptoms and Root Cause sections

### 4.4 Embedding Service

**Class**: `EmbeddingService`
**Responsibilities**:
- Generate vector embeddings using LLM embedding model
- Support multiple embedding models (OpenAI, open-source)
- Batch processing for efficiency
- Cache embeddings to avoid regeneration

**Key Methods**:
```java
public interface EmbeddingService {
    List<Float> generateEmbedding(String text);
    List<List<Float>> generateEmbeddings(List<String> texts);
    int getEmbeddingDimension();
}
```

**Configuration**:
- Embedding model: `text-embedding-3-large` (OpenAI) or `sentence-transformers/all-MiniLM-L6-v2`
- Embedding dimension: 1536 (OpenAI) or 384 (open-source)
- Batch size: 100

### 4.5 Vector Database Service

**Class**: `VectorStoreService`
**Responsibilities**:
- Store and index vector embeddings
- Perform similarity search (cosine similarity)
- Manage metadata alongside vectors
- Support filtering by metadata (space, date, etc.)

**Technology Choice**: **PostgreSQL with PGVector Extension**

**Rationale**:
- PostgreSQL extension providing native vector support
- Native integration with Spring Data JPA
- ACID compliance for data consistency
- Excellent performance for small to medium datasets (up to ~100K documents)
- Unified storage for both vectors and metadata
- Standard SQL queries with vector similarity search
- Open source and self-hostable
- Mature and well-documented

**Future Scalability**: If the system grows beyond 100K documents or requires very high query volume, consider migrating to Milvus while keeping PostgreSQL for metadata.

**Data Schema** (PostgreSQL with PGVector):
```sql
CREATE TABLE rca_embeddings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    page_id VARCHAR(255) NOT NULL,
    chunk_index INTEGER NOT NULL,
    chunk_type VARCHAR(50), -- 'SYMPTOMS' or 'ROOT_CAUSE'
    content TEXT NOT NULL,
    embedding vector(1536), -- Adjust based on model
    metadata JSONB,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

CREATE INDEX ON rca_embeddings USING ivfflat (embedding vector_cosine_ops);
CREATE INDEX ON rca_embeddings (page_id);
CREATE INDEX ON rca_embeddings (chunk_type);

-- Note: IVFFlat index provides fast approximate nearest neighbor search
-- For exact search, use vector_l2_ops or vector_ip_ops instead of vector_cosine_ops
```

**PGVector Setup Requirements**:
1. Install PostgreSQL 11+ (recommended: PostgreSQL 14+)
2. Install PGVector extension:
   ```sql
   CREATE EXTENSION IF NOT EXISTS vector;
   ```
3. The `vector` data type supports cosine, L2, and inner product similarity
4. IVFFlat index provides approximate nearest neighbor search (faster, slight accuracy trade-off)

### 4.6 Search Service

**Class**: `SearchService`
**Responsibilities**:
- Convert user query to embedding
- Perform vector similarity search
- Rank results by relevance score
- Filter by permissions (user access)
- Return top-K results

**Key Methods**:
```java
public interface SearchService {
    List<SearchResult> searchSimilarRCAs(String query, int topK, String userId);
    List<SearchResult> searchBySymptoms(String symptoms, int topK);
    List<SearchResult> searchByRootCause(String rootCause, int topK);
}
```

**Data Model**:
```java
public class SearchResult {
    private String pageId;
    private String title;
    private String content;
    private String confluenceUrl;
    private Double similarityScore;
    private String chunkType; // SYMPTOMS or ROOT_CAUSE
    private ParsedRCA fullRCA;
}
```

### 4.7 LLM Service

**Class**: `LLMService`
**Responsibilities**:
- Generate synthesized summaries from retrieved RCAs
- Format responses with citations
- Handle "no results found" scenarios
- Prevent hallucination

**Key Methods**:
```java
public interface LLMService {
    String generateSummary(String userQuery, List<SearchResult> results);
    String synthesizeRootCause(List<SearchResult> results);
}
```

**Prompt Template**:
```
You are an expert SRE analyzing incident reports. Based on the following historical Root Cause Analysis documents, suggest a potential root cause for the current issue.

Current Issue:
{userQuery}

Historical RCAs:
{retrievedRCAs}

Instructions:
1. Analyze the symptoms and root causes from historical RCAs
2. Identify patterns and similarities
3. Suggest the most likely root cause
4. If no relevant historical data exists, state: "No similar historical incidents found."
5. Always cite the source RCA documents

Format your response as:
- Suggested Root Cause: [your analysis]
- Confidence: [High/Medium/Low]
- Similar Historical Incidents:
  - [RCA Title] (Link: [URL])
  - [RCA Title] (Link: [URL])
```

### 4.8 Metadata Store (PostgreSQL)

**Schema Design**:

```sql
-- RCA Pages Metadata
CREATE TABLE rca_pages (
    page_id VARCHAR(255) PRIMARY KEY,
    space_key VARCHAR(255) NOT NULL,
    title VARCHAR(500) NOT NULL,
    url TEXT NOT NULL,
    tags TEXT[],
    last_modified TIMESTAMP NOT NULL,
    ingested_at TIMESTAMP DEFAULT NOW(),
    parsed_at TIMESTAMP,
    embedding_generated_at TIMESTAMP,
    status VARCHAR(50) DEFAULT 'PENDING', -- PENDING, PARSED, EMBEDDED, ERROR
    error_message TEXT
);

-- Parsed RCA Data
CREATE TABLE parsed_rca (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    page_id VARCHAR(255) REFERENCES rca_pages(page_id),
    symptoms TEXT,
    root_cause TEXT,
    resolution TEXT,
    incident_date TIMESTAMP,
    parsed_content JSONB,
    created_at TIMESTAMP DEFAULT NOW()
);

-- Sync History
CREATE TABLE sync_history (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    sync_type VARCHAR(50), -- FULL, INCREMENTAL
    spaces_synced TEXT[],
    pages_fetched INTEGER,
    pages_processed INTEGER,
    pages_failed INTEGER,
    started_at TIMESTAMP,
    completed_at TIMESTAMP,
    status VARCHAR(50) -- RUNNING, COMPLETED, FAILED
);

-- User Permissions Cache
CREATE TABLE user_permissions (
    user_id VARCHAR(255),
    space_key VARCHAR(255),
    has_access BOOLEAN,
    cached_at TIMESTAMP,
    PRIMARY KEY (user_id, space_key)
);
```

## 5. API Design

### 5.1 REST Endpoints

#### 5.1.1 Search Endpoints

**POST /api/v1/search**
```json
Request:
{
  "query": "Database connection pool exhausted",
  "topK": 5,
  "filterBy": {
    "spaceKeys": ["ENG", "OPS"],
    "dateRange": {
      "from": "2024-01-01",
      "to": "2024-12-31"
    }
  }
}

Response:
{
  "query": "Database connection pool exhausted",
  "results": [
    {
      "pageId": "12345",
      "title": "RCA: Database Pool Exhaustion - Q1 2024",
      "similarityScore": 0.92,
      "suggestedRootCause": "Connection pool configuration issue",
      "confluenceUrl": "https://confluence.example.com/pages/12345",
      "symptoms": "Users reported 503 errors...",
      "rootCause": "Connection pool max size was set too low...",
      "resolution": "Increased pool size from 10 to 50..."
    }
  ],
  "summary": {
    "suggestedRootCause": "Based on RCAs #102 and #45, the root cause is likely connection pool exhaustion due to misconfiguration.",
    "confidence": "High",
    "similarIncidents": 3
  },
  "executionTimeMs": 1200
}
```

**POST /api/v1/search/symptoms**
- Similar to above but specifically searches symptom embeddings

**POST /api/v1/search/root-cause**
- Similar to above but specifically searches root cause embeddings

#### 5.1.2 Ingestion Endpoints

**POST /api/v1/ingestion/sync**
```json
Request:
{
  "spaceKeys": ["ENG", "OPS"],
  "syncType": "INCREMENTAL", // or "FULL"
  "tags": ["rca", "post-mortem"]
}

Response:
{
  "syncId": "uuid",
  "status": "RUNNING",
  "estimatedCompletionTime": "2024-11-22T18:30:00Z"
}
```

**GET /api/v1/ingestion/sync/{syncId}**
- Get sync status and progress

**POST /api/v1/ingestion/page/{pageId}**
- Manually trigger ingestion for a specific page

#### 5.1.3 Management Endpoints

**GET /api/v1/stats**
- System statistics (total RCAs, last sync, etc.)

**GET /api/v1/health**
- Health check (already implemented)

**GET /api/v1/pages/{pageId}**
- Get parsed RCA data for a specific page

## 6. Data Flow

### 6.1 Ingestion Flow

```
1. Scheduled Job Triggered (Every 24 hours)
   ↓
2. ConfluenceService.fetchModifiedPagesSince(lastSync)
   ↓
3. Filter by tags (rca, post-mortem)
   ↓
4. For each page:
   a. DocumentParserService.parseDocument(page)
   b. Extract Symptoms, Root Cause, Resolution
   c. ChunkingService.chunk(content)
   d. EmbeddingService.generateEmbeddings(chunks)
   e. VectorStoreService.store(embeddings, metadata)
   f. Save to PostgreSQL
   ↓
5. Update sync_history
```

### 6.2 Query Flow (RAG)

```
1. User submits query via API
   ↓
2. EmbeddingService.generateEmbedding(query)
   ↓
3. VectorStoreService.similaritySearch(queryEmbedding, topK=10)
   ↓
4. Filter results by user permissions
   ↓
5. Retrieve full RCA documents for top results
   ↓
6. LLMService.generateSummary(query, results)
   ↓
7. Format response with citations
   ↓
8. Return to user
```

## 7. Technology Stack

### 7.1 Core Framework
- **Java 25** - Programming language
- **Spring Boot 4.0** - Application framework
- **Spring Data JPA** - Database access
- **Spring Web** - REST API

### 7.2 External Services
- **Confluence REST API** - Data source
- **OpenAI API** - Embeddings (text-embedding-3-large) and LLM (GPT-4)
- **PostgreSQL 14+ with PGVector Extension** - Vector storage and metadata (primary database)
- **OkHttp** - HTTP client for API calls

### 7.3 Libraries
- **Gson 2.11.0** - JSON processing
- **Jsoup** - HTML parsing and cleaning
- **LangChain4j** (optional) - RAG framework helpers
- **SLF4J & Logback** - Logging

### 7.4 Infrastructure
- **Docker** - Containerization
- **Gradle** - Build tool
- **Git** - Version control

## 8. Security Design

### 8.1 Authentication & Authorization
- **Confluence API**: OAuth 2.0 or Personal Access Tokens (PAT)
- **User Permissions**: Cache Confluence space permissions per user
- **API Security**: Spring Security with JWT tokens (future)

### 8.2 Data Privacy
- Respect Confluence space-level permissions
- Filter search results based on user access
- Audit log for all searches

### 8.3 Secrets Management
- Store API keys in environment variables or secrets manager
- Never commit credentials to repository
- Use Spring Boot's `@ConfigurationProperties` for config

## 9. Performance Requirements

### 9.1 Latency Targets
- **Search API**: < 5 seconds (P0)
- **Embedding Generation**: < 2 seconds per batch
- **Vector Search**: < 1 second for top-K results

### 9.2 Scalability
- Support 10,000+ RCA documents
- Handle 100+ concurrent searches
- Incremental sync to minimize load

### 9.3 Caching Strategy
- Cache embeddings (no regeneration needed)
- Cache user permissions (TTL: 1 hour)
- Cache frequently accessed RCA documents

## 10. Error Handling

### 10.1 Error Scenarios
1. **Confluence API failures**: Retry with exponential backoff
2. **LLM API failures**: Fallback to simple similarity search
3. **Vector DB failures**: Graceful degradation
4. **Parsing failures**: Log and continue with other pages

### 10.2 Monitoring
- Log all errors with context
- Track sync success/failure rates
- Monitor API response times
- Alert on critical failures

## 11. Implementation Phases

### Phase 1: MVP (Weeks 1-4)
- [ ] Confluence API integration
- [ ] Basic document parsing
- [ ] Embedding generation
- [ ] Vector storage (PGVector)
- [ ] Simple search API
- [ ] Web UI for search

### Phase 2: Enhancement (Weeks 5-6)
- [ ] LLM synthesis for summaries
- [ ] Incremental sync
- [ ] User permissions filtering
- [ ] Improved parsing with LLM

### Phase 3: Production (Weeks 7-8)
- [ ] Performance optimization
- [ ] Error handling and retries
- [ ] Monitoring and logging
- [ ] Documentation
- [ ] Load testing

## 12. Testing Strategy

### 12.1 Unit Tests
- Service layer logic
- Parsing logic
- Embedding generation
- Vector search algorithms

### 12.2 Integration Tests
- Confluence API integration
- Vector database operations
- End-to-end search flow

### 12.3 Performance Tests
- Load testing for search API
- Sync performance with large datasets
- Vector search query performance

## 13. Deployment Architecture

### 13.1 Development
- Local PostgreSQL with PGVector
- Local Spring Boot application
- Mock Confluence API (for testing)

### 13.2 Production
```
┌─────────────────┐
│  Load Balancer  │
└────────┬────────┘
         │
    ┌────┴────┐
    │         │
┌───▼───┐ ┌──▼───┐
│ App 1 │ │ App 2│ (Spring Boot instances)
└───┬───┘ └──┬───┘
    │        │
    └───┬────┘
        │
   ┌────▼──────────┐
   │  PostgreSQL   │
   │  + PGVector   │
   │  (Primary)    │
   └───────┬───────┘
           │
   ┌───────▼───────┐
   │  PostgreSQL   │
   │  (Replica)     │
   └───────────────┘
```

**Production Considerations**:
- PostgreSQL primary-replica setup for high availability
- Connection pooling (HikariCP)
- Regular backups of vector data
- Monitor PGVector index performance
- Consider read replicas for scaling search queries

## 14. Configuration

### 14.1 Application Properties

```properties
# Confluence Configuration
confluence.base-url=https://your-domain.atlassian.net
confluence.auth.token=${CONFLUENCE_PAT}
confluence.spaces=ENG,OPS
confluence.tags=rca,post-mortem
confluence.sync.schedule=0 0 2 * * ? # Daily at 2 AM

# Embedding Configuration
embedding.model=text-embedding-3-large
embedding.dimension=1536
embedding.batch-size=100
embedding.api-key=${OPENAI_API_KEY}

# LLM Configuration
llm.model=gpt-4
llm.api-key=${OPENAI_API_KEY}
llm.temperature=0.3
llm.max-tokens=1000

# Database Configuration (PostgreSQL with PGVector)
spring.datasource.url=jdbc:postgresql://localhost:5432/rca_engine
spring.datasource.username=${DB_USER}
spring.datasource.password=${DB_PASSWORD}
spring.datasource.driver-class-name=org.postgresql.Driver
spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect
spring.jpa.hibernate.ddl-auto=validate
spring.jpa.show-sql=false

# PGVector Configuration
vector-db.type=pgvector
vector-db.embedding-dimension=1536
vector-db.index-type=ivfflat
vector-db.lists=100  # IVFFlat parameter (adjust based on dataset size)

# Chunking Configuration
chunking.size=800
chunking.overlap=150
chunking.strategy=semantic

# Search Configuration
search.default-top-k=5
search.max-top-k=20
search.min-similarity-score=0.7
```

## 15. Success Metrics

### 15.1 KPIs
- **Hit Rate**: % of searches where user clicks on suggested RCA
- **Resolution Speed**: Reduction in MTTR for repeat incidents
- **User Feedback**: Thumbs up/down on suggestions
- **System Uptime**: > 99.9%
- **Search Latency**: P95 < 5 seconds

### 15.2 Monitoring
- Track all searches and results
- Monitor user feedback
- Measure MTTR improvements
- Track system performance metrics

## 16. Future Enhancements (Out of Scope for MVP)

1. **Real-time Integration**: Auto-trigger searches from monitoring tools
2. **Jira Integration**: Browser extension or sidebar widget
3. **Auto-fix Scripts**: Automatic execution of resolution steps
4. **RCA Generation**: Auto-generate new RCA documents
5. **Multi-language Support**: Support for non-English RCAs
6. **Advanced Analytics**: Trend analysis, pattern detection

## 17. Risks and Mitigations

| Risk | Impact | Mitigation |
|------|--------|------------|
| Confluence API rate limits | High | Implement rate limiting and retry logic |
| LLM API costs | Medium | Cache embeddings, optimize prompts |
| Parsing accuracy | High | Use LLM for complex parsing, manual review |
| Vector DB performance | Medium | Optimize PGVector indexes, monitor query performance |
| Data privacy concerns | High | Strict permission filtering, audit logs |

## 18. Appendix

### 18.1 Key Design Decisions

1. **PostgreSQL + PGVector**: Chosen for native vector support, ACID compliance, and unified storage for vectors and metadata. Provides excellent performance for MVP and scales well up to ~100K documents.

2. **OpenAI Embeddings**: Chosen for quality (text-embedding-3-large). Can switch to open-source alternatives (e.g., sentence-transformers) later if needed.

3. **Java + Spring Boot**: Aligns with existing infrastructure and provides robust enterprise features.

4. **Incremental Sync**: Reduces load on Confluence API and improves efficiency by only processing modified pages.

5. **IVFFlat Index**: Chosen for approximate nearest neighbor search - provides good balance between speed and accuracy for MVP.

### 18.2 PostgreSQL + PGVector Setup Guide

**Prerequisites**:
- PostgreSQL 11+ (recommended: PostgreSQL 14+)
- PGVector extension

**Installation Steps**:

1. **Install PGVector Extension**:
   ```bash
   # On Ubuntu/Debian
   sudo apt-get install postgresql-14-pgvector
   
   # Or build from source
   git clone --branch v0.5.1 https://github.com/pgvector/pgvector.git
   cd pgvector
   make
   sudo make install
   ```

2. **Enable Extension in Database**:
   ```sql
   CREATE DATABASE rca_engine;
   \c rca_engine
   CREATE EXTENSION IF NOT EXISTS vector;
   ```

3. **Verify Installation**:
   ```sql
   SELECT * FROM pg_extension WHERE extname = 'vector';
   ```

4. **Create Tables** (see schema in Section 4.5)

**Performance Tuning**:
- Adjust `lists` parameter in IVFFlat index based on dataset size: `lists = rows / 1000` (minimum 10)
- For exact search, use `vector_l2_ops` or `vector_ip_ops` instead of `vector_cosine_ops`
- Monitor query performance and adjust index parameters as needed

### 18.3 References
- Confluence REST API: https://developer.atlassian.com/cloud/confluence/rest/
- PGVector: https://github.com/pgvector/pgvector
- OpenAI Embeddings: https://platform.openai.com/docs/guides/embeddings
- RAG Best Practices: https://www.pinecone.io/learn/retrieval-augmented-generation/

---

**Document Status**: Draft - Ready for Review
**Next Steps**: 
1. Review and approve design
2. Create detailed task breakdown
3. Begin Phase 1 implementation

