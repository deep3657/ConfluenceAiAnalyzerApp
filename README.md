# RCA Insight Engine (Project "Recall")

AI-powered Root Cause Analysis engine that uses RAG (Retrieval-Augmented Generation) to provide intelligent root cause suggestions based on historical Confluence RCA documents.

## Architecture

This is a full-stack application consisting of:

- **Backend**: Spring Boot REST API (Java 25, Gradle, PostgreSQL + PGVector)
- **Frontend**: React + TypeScript UI (Vite, Tailwind CSS)

## Requirements

- Java 25 (OpenJDK 25.0.1)
- Gradle 9.2.1 (wrapper included)
- Spring Boot 3.4.0

## Project Structure

```
ConfluenceAiAnalyser/
├── build.gradle          # Gradle build configuration
├── settings.gradle       # Gradle settings
├── gradle.properties     # Gradle properties
├── gradlew               # Gradle wrapper script
├── gradlew.bat           # Gradle wrapper for Windows
├── gradle/               # Gradle wrapper files
└── src/
    ├── main/
    │   ├── java/
    │   │   └── com/confluenceai/analyzer/
    │   │       └── ConfluenceAiAnalyzer.java
    │   └── resources/
    │       └── logback.xml
    └── test/
        └── java/
            └── com/confluenceai/analyzer/
```

## Building the Project

```bash
./gradlew build
```

## Running the Application

### Using Gradle (Recommended)

```bash
./gradlew bootRun
```

The application will start on `http://localhost:8080`

### Running the JAR directly

After building, you can run the JAR file:

```bash
./gradlew build
java -Dspring.classformat.ignore=true -jar build/libs/ConfluenceAiAnalyser-1.0.0.jar
```

**Note:** The `-Dspring.classformat.ignore=true` system property is required when using Java 25, as Spring Boot 3.4.0 doesn't fully support Java 25's class file format yet. This property is automatically set when using `./gradlew bootRun`.

## Health Check API

The application includes a health check endpoint:

- **Custom Health Check**: `GET http://localhost:8080/api/health`
  - Returns application status, timestamp, and service name
  
- **Spring Boot Actuator Health**: `GET http://localhost:8080/actuator/health`
  - Returns Spring Boot's built-in health status

### Example Response

```json
{
  "status": "UP",
  "timestamp": "2024-11-22T10:30:00",
  "service": "Confluence AI Analyzer"
}
```

## Dependencies

- **Spring Boot 3.4.0** - Application framework
  - Spring Boot Web Starter - REST API support
  - Spring Boot Actuator - Health checks and monitoring
- **OkHttp 4.12.0** - HTTP client for Confluence REST API calls
- **Gson 2.10.1** - JSON processing
- **OpenAI Java Client 0.18.2** - AI/ML integration
- **SLF4J & Logback** - Logging framework
- **JUnit 5** - Testing framework
- **Mockito** - Mocking framework for tests

## Configuration

- **Spring Boot Configuration**: `src/main/resources/application.properties`
  - Server port: 8080
  - Actuator endpoints configured
- **Logging**: `src/main/resources/logback.xml`

## Quick Start

To start both applications quickly:

```bash
# Option 1: Use the start script
./start-apps.sh

# Option 2: Manual start (see START_APPLICATION.md for details)
# Terminal 1 - Backend
./gradlew bootRun

# Terminal 2 - Frontend
cd frontend && npm run dev
```

**See [START_APPLICATION.md](START_APPLICATION.md) for detailed step-by-step instructions.**

## Frontend

The frontend is a React + TypeScript application located in the `frontend/` directory.

### Getting Started

```bash
cd frontend
npm install
npm run dev
```

The frontend will be available at `http://localhost:3000` and automatically proxies API requests to the backend.

See [frontend/README.md](frontend/README.md) for detailed frontend documentation.

### Features

- **Search Interface**: Semantic search for RCA documents with AI-powered suggestions
- **Ingestion Management**: Sync and manage Confluence data ingestion
- **Statistics Dashboard**: View system statistics and processing status
- **Real-time Updates**: Auto-refreshing stats and sync status polling

## License

[Add your license here]

