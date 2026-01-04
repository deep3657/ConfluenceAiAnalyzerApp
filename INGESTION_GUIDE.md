# How to Ingest RCA Data from Confluence

This guide explains how to ingest Root Cause Analysis (RCA) documents from Confluence into the RCA Insight Engine.

## Prerequisites

Before ingesting data, you need:

1. **Confluence Access**
   - Access to the Confluence instance where RCA documents are stored
   - Permission to read pages in the target spaces

2. **Confluence API Credentials**
   - **Personal Access Token (PAT)** - Recommended
   - Or **OAuth 2.0** credentials (if configured)

3. **Configuration**
   - Confluence base URL
   - Target space keys (e.g., "ENG", "OPS")
   - Tags to filter RCA pages (e.g., "rca", "post-mortem")

## Step 1: Get Confluence API Credentials

### Option A: Personal Access Token (Recommended)

1. Log in to your Confluence instance
2. Go to **Account Settings** → **Security** → **Personal Access Tokens**
3. Click **Create token**
4. Give it a name (e.g., "RCA Insight Engine")
5. Set expiration (or leave as "No expiration")
6. Copy the token (you won't see it again!)

### Option B: Username/Password (Not Recommended)

- Use your Confluence username and password
- Less secure, not recommended for production

## Step 2: Configure the Application

### Method 1: Environment Variables (Recommended)

Set environment variables before starting the application:

```bash
# Confluence Configuration
export CONFLUENCE_BASE_URL="https://your-confluence-instance.atlassian.net"
export CONFLUENCE_PAT="your-personal-access-token-here"

# Optional: Override default spaces and tags
export CONFLUENCE_SPACES="ENG,OPS"
export CONFLUENCE_TAGS="rca,post-mortem"

# OpenAI API Key (required for embeddings)
export OPENAI_API_KEY="your-openai-api-key-here"

# Database credentials (if different from defaults)
export DB_USER="saideepak.b"
export DB_PASSWORD="your-db-password"
```

Then start the application:
```bash
./gradlew bootRun
```

### Method 2: Update application.properties

Edit `src/main/resources/application.properties`:

```properties
# Confluence Configuration
confluence.base-url=https://your-confluence-instance.atlassian.net
confluence.auth.token=your-personal-access-token-here
confluence.spaces=ENG,OPS
confluence.tags=rca,post-mortem

# Embedding Configuration
embedding.api-key=your-openai-api-key-here
```

**⚠️ Security Warning**: Don't commit credentials to git! Use environment variables or a separate config file.

## Step 3: Ingest Data

### Option A: Using the Web UI (Easiest)

1. **Start the application** (if not already running):
   ```bash
   ./gradlew bootRun  # Backend
   cd frontend && npm run dev  # Frontend
   ```

2. **Open the UI**: http://localhost:3000

3. **Navigate to Ingestion Page**:
   - Click on "Ingestion" in the navigation bar

4. **Configure Sync**:
   - **Sync Type**: 
     - `INCREMENTAL` - Only fetch new/updated pages (faster, recommended)
     - `FULL` - Fetch all pages (slower, use for initial setup)
   - **Spaces**: Comma-separated list (e.g., "ENG, OPS")
   - **Tags**: Comma-separated list (e.g., "rca, post-mortem")

5. **Start Sync**:
   - Click "Start Sync" button
   - Monitor progress in real-time
   - View sync status, pages fetched, processed, and failed

### Option B: Using the REST API

#### Start a Sync Operation

```bash
curl -X POST http://localhost:8080/api/v1/ingestion/sync \
  -H "Content-Type: application/json" \
  -d '{
    "syncType": "INCREMENTAL",
    "spaces": ["ENG", "OPS"],
    "tags": ["rca", "post-mortem"]
  }'
```

Response:
```json
{
  "syncId": "uuid-here",
  "status": "RUNNING",
  "message": "Sync started",
  "pagesFetched": 0,
  "pagesProcessed": 0,
  "pagesFailed": 0,
  "startedAt": "2024-11-26T17:00:00"
}
```

#### Check Sync Status

```bash
curl http://localhost:8080/api/v1/ingestion/sync/{syncId}
```

#### Ingest a Specific Page

```bash
curl -X POST http://localhost:8080/api/v1/ingestion/page/{pageId}
```

### Option C: Using Swagger UI

1. Open http://localhost:8080/swagger-ui.html
2. Find the **Ingestion** section
3. Use the `/api/v1/ingestion/sync` endpoint
4. Click "Try it out"
5. Enter your sync parameters
6. Execute and view the response

## Step 4: Monitor Ingestion Progress

### In the UI

- **Ingestion Page**: Shows active sync operations with real-time progress
- **Statistics Page**: View total pages, status breakdown, and processing pipeline

### Via API

```bash
# Get statistics
curl http://localhost:8080/api/v1/stats

# Response:
{
  "totalPages": 150,
  "pagesByStatus": {
    "PENDING": 10,
    "PARSED": 80,
    "EMBEDDED": 55,
    "ERROR": 5
  }
}
```

### Check Database Directly

```bash
psql -h localhost -U saideepak.b -d rca_engine

# Count pages
SELECT COUNT(*) FROM rca_pages;

# View recent pages
SELECT page_id, title, status, ingested_at 
FROM rca_pages 
ORDER BY ingested_at DESC 
LIMIT 10;

# Check sync history
SELECT * FROM sync_history 
ORDER BY started_at DESC 
LIMIT 5;
```

## What Happens During Ingestion

The ingestion process follows these steps:

1. **Fetch Pages**: 
   - Connects to Confluence API
   - Fetches pages matching spaces and tags
   - Filters by last modified date (for incremental sync)

2. **Parse Content**:
   - Extracts HTML content from Confluence pages
   - Parses to identify:
     - Symptoms
     - Root Cause
     - Resolution
     - Incident Date

3. **Generate Embeddings**:
   - Splits content into chunks
   - Generates vector embeddings using OpenAI
   - Stores embeddings in PostgreSQL with PGVector

4. **Store Metadata**:
   - Saves page metadata (title, URL, tags, etc.)
   - Links parsed RCA data
   - Updates sync history

## Troubleshooting

### Issue: "Authentication failed"

**Solution**:
- Verify your Personal Access Token is correct
- Check token hasn't expired
- Ensure token has read permissions for target spaces

### Issue: "No pages found"

**Possible Causes**:
- Spaces don't exist or you don't have access
- Tags don't match any pages
- Pages don't have the specified tags

**Solution**:
- Verify space keys are correct
- Check page tags in Confluence
- Try a FULL sync to see all pages

### Issue: "OpenAI API error"

**Solution**:
- Verify `OPENAI_API_KEY` is set correctly
- Check API key has sufficient credits
- Verify embedding model name is correct

### Issue: "Database connection error"

**Solution**:
- Ensure PostgreSQL is running
- Verify database credentials
- Check database exists: `psql -l | grep rca_engine`

### Issue: "Parsing failed for some pages"

**Expected Behavior**:
- Some pages may fail parsing if format is unexpected
- Check error messages in the UI or database
- Pages with errors are marked with status "ERROR"

## Best Practices

1. **Start with INCREMENTAL sync** to test the connection
2. **Use specific tags** to filter relevant RCA documents
3. **Monitor the first sync** to ensure pages are being processed correctly
4. **Check error messages** for pages that fail
5. **Run FULL sync periodically** (e.g., weekly) to catch any missed updates
6. **Set up scheduled syncs** using the scheduler (configured in application.properties)

## Scheduled Syncs

The application can automatically sync on a schedule. Configure in `application.properties`:

```properties
confluence.sync.schedule=0 0 2 * * ?  # Daily at 2 AM
```

Cron format: `second minute hour day month weekday`

Examples:
- `0 0 2 * * ?` - Daily at 2 AM
- `0 0 */6 * * ?` - Every 6 hours
- `0 0 0 * * MON` - Every Monday at midnight

## Next Steps

After ingesting data:

1. **Test Search**: Use the Search page to find similar RCAs
2. **View Statistics**: Check the Statistics page for ingestion metrics
3. **Review Data**: Verify pages were parsed correctly
4. **Set Up Scheduled Syncs**: Configure automatic updates

## Example: Complete Ingestion Workflow

```bash
# 1. Set environment variables
export CONFLUENCE_BASE_URL="https://mycompany.atlassian.net"
export CONFLUENCE_PAT="ATATT3xFfGF0..."
export OPENAI_API_KEY="sk-..."
export CONFLUENCE_SPACES="ENG,OPS"
export CONFLUENCE_TAGS="rca,post-mortem"

# 2. Start backend
./gradlew bootRun

# 3. In another terminal, start sync via API
curl -X POST http://localhost:8080/api/v1/ingestion/sync \
  -H "Content-Type: application/json" \
  -d '{
    "syncType": "FULL",
    "spaces": ["ENG", "OPS"],
    "tags": ["rca", "post-mortem"]
  }'

# 4. Check status
curl http://localhost:8080/api/v1/stats

# 5. Monitor in UI at http://localhost:3000
```

## Support

If you encounter issues:

1. Check application logs: `tail -f backend.log`
2. Verify credentials are correct
3. Test Confluence API access manually
4. Check database connection
5. Review error messages in the UI or API responses







