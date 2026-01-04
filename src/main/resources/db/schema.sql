-- =====================================================
-- RCA Insight Engine Database Schema
-- PostgreSQL 14+ with PGVector Extension
-- =====================================================

-- Ensure PGVector extension is enabled
CREATE EXTENSION IF NOT EXISTS vector;

-- =====================================================
-- 1. RCA Pages Metadata Table
-- Stores metadata about Confluence pages
-- =====================================================
CREATE TABLE IF NOT EXISTS rca_pages (
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
    error_message TEXT,
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_rca_pages_space_key ON rca_pages(space_key);
CREATE INDEX IF NOT EXISTS idx_rca_pages_status ON rca_pages(status);
CREATE INDEX IF NOT EXISTS idx_rca_pages_last_modified ON rca_pages(last_modified);
CREATE INDEX IF NOT EXISTS idx_rca_pages_tags ON rca_pages USING GIN(tags);

-- =====================================================
-- 2. Parsed RCA Data Table
-- Stores extracted structured data from RCA documents
-- =====================================================
CREATE TABLE IF NOT EXISTS parsed_rca (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    page_id VARCHAR(255) NOT NULL REFERENCES rca_pages(page_id) ON DELETE CASCADE,
    symptoms TEXT,
    root_cause TEXT,
    resolution TEXT,
    incident_date TIMESTAMP,
    parsed_content JSONB,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_parsed_rca_page_id ON parsed_rca(page_id);
CREATE INDEX IF NOT EXISTS idx_parsed_rca_incident_date ON parsed_rca(incident_date);

-- =====================================================
-- 3. RCA Embeddings Table
-- Stores vector embeddings for semantic search
-- =====================================================
CREATE TABLE IF NOT EXISTS rca_embeddings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    page_id VARCHAR(255) NOT NULL REFERENCES rca_pages(page_id) ON DELETE CASCADE,
    chunk_index INTEGER NOT NULL,
    chunk_type VARCHAR(50) NOT NULL, -- 'SYMPTOMS' or 'ROOT_CAUSE'
    content TEXT NOT NULL,
    embedding vector(768), -- Gemini text-embedding-004 dimension (768) or OpenAI (1536)
    metadata JSONB,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),
    CONSTRAINT unique_page_chunk UNIQUE(page_id, chunk_index, chunk_type)
);

-- Vector similarity search index (IVFFlat for approximate nearest neighbor)
-- Note: Create index after inserting some data for better performance
-- Lists parameter: adjust based on dataset size (rows / 1000, minimum 10)
CREATE INDEX IF NOT EXISTS idx_rca_embeddings_vector ON rca_embeddings 
    USING ivfflat (embedding vector_cosine_ops) WITH (lists = 100);

-- Standard indexes for filtering
CREATE INDEX IF NOT EXISTS idx_rca_embeddings_page_id ON rca_embeddings(page_id);
CREATE INDEX IF NOT EXISTS idx_rca_embeddings_chunk_type ON rca_embeddings(chunk_type);
CREATE INDEX IF NOT EXISTS idx_rca_embeddings_metadata ON rca_embeddings USING GIN(metadata);

-- =====================================================
-- 4. Sync History Table
-- Tracks ingestion sync operations
-- =====================================================
CREATE TABLE IF NOT EXISTS sync_history (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    sync_type VARCHAR(50) NOT NULL, -- FULL, INCREMENTAL
    spaces_synced TEXT[],
    pages_fetched INTEGER DEFAULT 0,
    pages_processed INTEGER DEFAULT 0,
    pages_failed INTEGER DEFAULT 0,
    started_at TIMESTAMP DEFAULT NOW(),
    completed_at TIMESTAMP,
    status VARCHAR(50) DEFAULT 'RUNNING', -- RUNNING, COMPLETED, FAILED
    error_message TEXT,
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_sync_history_status ON sync_history(status);
CREATE INDEX IF NOT EXISTS idx_sync_history_started_at ON sync_history(started_at);
CREATE INDEX IF NOT EXISTS idx_sync_history_sync_type ON sync_history(sync_type);

-- =====================================================
-- 5. User Permissions Cache Table
-- Caches Confluence space permissions per user
-- =====================================================
CREATE TABLE IF NOT EXISTS user_permissions (
    user_id VARCHAR(255) NOT NULL,
    space_key VARCHAR(255) NOT NULL,
    has_access BOOLEAN NOT NULL DEFAULT FALSE,
    cached_at TIMESTAMP DEFAULT NOW(),
    expires_at TIMESTAMP, -- TTL for cache invalidation
    PRIMARY KEY (user_id, space_key)
);

CREATE INDEX IF NOT EXISTS idx_user_permissions_user_id ON user_permissions(user_id);
CREATE INDEX IF NOT EXISTS idx_user_permissions_space_key ON user_permissions(space_key);
CREATE INDEX IF NOT EXISTS idx_user_permissions_expires_at ON user_permissions(expires_at);

-- =====================================================
-- 6. Helper Functions and Views
-- =====================================================

-- View: RCA Summary with latest status
CREATE OR REPLACE VIEW v_rca_summary AS
SELECT 
    rp.page_id,
    rp.space_key,
    rp.title,
    rp.url,
    rp.tags,
    rp.status,
    rp.last_modified,
    rp.ingested_at,
    pr.symptoms,
    pr.root_cause,
    pr.incident_date,
    COUNT(re.id) as embedding_count,
    MAX(re.created_at) as last_embedding_created
FROM rca_pages rp
LEFT JOIN parsed_rca pr ON rp.page_id = pr.page_id
LEFT JOIN rca_embeddings re ON rp.page_id = re.page_id
GROUP BY rp.page_id, rp.space_key, rp.title, rp.url, rp.tags, rp.status, 
         rp.last_modified, rp.ingested_at, pr.symptoms, pr.root_cause, pr.incident_date;

-- Function: Clean expired permission cache entries
CREATE OR REPLACE FUNCTION clean_expired_permissions()
RETURNS INTEGER AS $$
DECLARE
    deleted_count INTEGER;
BEGIN
    DELETE FROM user_permissions 
    WHERE expires_at IS NOT NULL AND expires_at < NOW();
    GET DIAGNOSTICS deleted_count = ROW_COUNT;
    RETURN deleted_count;
END;
$$ LANGUAGE plpgsql;

-- =====================================================
-- 7. Comments for Documentation
-- =====================================================

COMMENT ON TABLE rca_pages IS 'Metadata about Confluence pages containing RCA documents';
COMMENT ON TABLE parsed_rca IS 'Structured data extracted from RCA documents (symptoms, root cause, resolution)';
COMMENT ON TABLE rca_embeddings IS 'Vector embeddings for semantic search. Each chunk (symptoms/root cause) is embedded separately';
COMMENT ON TABLE sync_history IS 'History of Confluence sync operations';
COMMENT ON TABLE user_permissions IS 'Cached Confluence space permissions per user';

COMMENT ON COLUMN rca_embeddings.embedding IS 'Vector embedding of dimension 1536 (OpenAI text-embedding-3-large)';
COMMENT ON COLUMN rca_embeddings.chunk_type IS 'Type of chunk: SYMPTOMS or ROOT_CAUSE';
COMMENT ON COLUMN rca_pages.status IS 'Processing status: PENDING, PARSED, EMBEDDED, ERROR';

-- =====================================================
-- Schema Creation Complete
-- =====================================================

