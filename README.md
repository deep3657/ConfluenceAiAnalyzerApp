# Confluence AI Analyzer

A Java-based Spring Boot application for performing AI analysis on Confluence content.

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

## Next Steps

1. Implement Confluence REST API client to fetch content
2. Integrate AI analysis logic
3. Add configuration for Confluence credentials and AI API keys
4. Implement result processing and output

## License

[Add your license here]

