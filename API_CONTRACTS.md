# API Contracts Documentation

This document describes the API contracts available for UI integration.

## Accessing API Documentation

Once the application is running, you can access the interactive API documentation at:

- **Swagger UI**: `http://localhost:8080/swagger-ui.html`
- **OpenAPI JSON**: `http://localhost:8080/api-docs`
- **OpenAPI YAML**: `http://localhost:8080/api-docs.yaml`

## API Endpoints

### Base URL
```
http://localhost:8080
```

### Health Check
- **GET** `/api/health`
- Returns application health status

### Search Endpoints

#### Search Similar RCAs
- **POST** `/api/v1/search`
- **Request Body**:
  ```json
  {
    "query": "string",
    "topK": 5
  }
  ```
- **Response**:
  ```json
  {
    "query": "string",
    "results": [
      {
        "pageId": "string",
        "title": "string",
        "url": "string",
        "similarityScore": 0.85,
        "symptoms": "string",
        "rootCause": "string",
        "resolution": "string",
        "incidentDate": "2024-01-01T00:00:00"
      }
    ],
    "summary": {
      "suggestedRootCause": "string",
      "confidence": "High|Medium|Low",
      "similarIncidents": 5
    },
    "executionTimeMs": 150
  }
  ```

#### Search by Symptoms
- **POST** `/api/v1/search/symptoms`
- Same request/response format as above, but focuses on symptom matching

#### Search by Root Cause
- **POST** `/api/v1/search/root-cause`
- Same request/response format as above, but focuses on root cause matching

### Ingestion Endpoints

#### Start Sync
- **POST** `/api/v1/ingestion/sync`
- **Request Body**:
  ```json
  {
    "syncType": "FULL|INCREMENTAL",
    "spaces": ["ENG", "OPS"],
    "tags": ["rca", "post-mortem"]
  }
  ```
- **Response**:
  ```json
  {
    "syncId": "uuid",
    "status": "RUNNING|COMPLETED|FAILED",
    "message": "string",
    "pagesFetched": 0,
    "pagesProcessed": 0,
    "pagesFailed": 0,
    "startedAt": "2024-01-01T00:00:00",
    "completedAt": "2024-01-01T00:00:00"
  }
  ```

#### Get Sync Status
- **GET** `/api/v1/ingestion/sync/{syncId}`
- Returns the current status of a sync operation

#### Ingest Specific Page
- **POST** `/api/v1/ingestion/page/{pageId}`
- Triggers ingestion for a specific Confluence page

### Management Endpoints

#### Get Statistics
- **GET** `/api/v1/stats`
- **Response**:
  ```json
  {
    "totalPages": 100,
    "pagesByStatus": {
      "PENDING": 10,
      "PARSED": 50,
      "EMBEDDED": 35,
      "ERROR": 5
    }
  }
  ```

#### Get Page by ID
- **GET** `/api/v1/pages/{pageId}`
- Returns metadata for a specific RCA page

## Data Models

### SearchRequest
```json
{
  "query": "string (required)",
  "topK": "integer (optional, default: 5, max: 20)"
}
```

### SearchResult
```json
{
  "pageId": "string",
  "title": "string",
  "url": "string",
  "similarityScore": "number (0.0-1.0)",
  "symptoms": "string",
  "rootCause": "string",
  "resolution": "string",
  "incidentDate": "ISO 8601 datetime"
}
```

### SearchResponse
```json
{
  "query": "string",
  "results": "array of SearchResult",
  "summary": {
    "suggestedRootCause": "string",
    "confidence": "High|Medium|Low",
    "similarIncidents": "integer"
  },
  "executionTimeMs": "integer"
}
```

### SyncRequest
```json
{
  "syncType": "FULL|INCREMENTAL (required)",
  "spaces": "array of strings (optional)",
  "tags": "array of strings (optional)"
}
```

### SyncResponse
```json
{
  "syncId": "uuid",
  "status": "RUNNING|COMPLETED|FAILED",
  "message": "string",
  "pagesFetched": "integer",
  "pagesProcessed": "integer",
  "pagesFailed": "integer",
  "startedAt": "ISO 8601 datetime",
  "completedAt": "ISO 8601 datetime (nullable)"
}
```

## Error Responses

All endpoints may return the following error responses:

- **400 Bad Request**: Invalid request parameters
- **404 Not Found**: Resource not found
- **500 Internal Server Error**: Server error

Error response format:
```json
{
  "timestamp": "ISO 8601 datetime",
  "status": "integer",
  "error": "string",
  "message": "string",
  "path": "string"
}
```

## Authentication

Currently, the API does not require authentication. In production, this should be secured with:
- API keys
- OAuth 2.0
- JWT tokens

## Rate Limiting

Rate limiting is not currently implemented but should be added for production use.

## Testing the API

### Using Swagger UI
1. Start the application: `./gradlew bootRun`
2. Navigate to `http://localhost:8080/swagger-ui.html`
3. Use the interactive UI to test endpoints

### Using cURL

Example search request:
```bash
curl -X POST http://localhost:8080/api/v1/search \
  -H "Content-Type: application/json" \
  -d '{
    "query": "database connection timeout",
    "topK": 5
  }'
```

Example sync request:
```bash
curl -X POST http://localhost:8080/api/v1/ingestion/sync \
  -H "Content-Type: application/json" \
  -d '{
    "syncType": "INCREMENTAL",
    "spaces": ["ENG"],
    "tags": ["rca"]
  }'
```

## Integration Notes

1. **Async Operations**: Sync operations are asynchronous. Use the `syncId` returned from the POST request to poll for status.

2. **Pagination**: Search endpoints currently return all results up to `topK`. Future versions may support pagination.

3. **Caching**: Consider implementing client-side caching for frequently accessed pages and statistics.

4. **Error Handling**: Always check the HTTP status code and handle errors gracefully.

5. **Timeouts**: Set appropriate timeouts for API calls, especially for search operations which may take longer.

## Versioning

The API uses URL versioning (`/api/v1/`). Future versions will be available at `/api/v2/`, etc.

## Support

For questions or issues, contact the development team or refer to the Swagger UI documentation.

