# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Development Commands

### Build and Run
```bash
# Build the project
mvn clean compile

# Run the application
mvn spring-boot:run

# Package the application
mvn clean package

# Run tests
mvn test

# Run specific test class
mvn test -Dtest=ClassName

# Run with specific profile
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

### Database Access
- **Production**: MariaDB at `localhost:3306/tot` (username: `root`, password: `root`)
- **H2 Console**: http://localhost:8080/h2-console (enabled for development)
- **H2 Connection**: `jdbc:h2:mem:testdb` (username: `sa`, no password)

### API Documentation
- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **OpenAPI Docs**: http://localhost:8080/v3/api-docs
- **Application Port**: 8080

## Architecture Overview

This is a Tree of Thought (ToT) scheduling system that combines AI reasoning with autonomous task execution:

**Core Flow**: Scheduler → ScheduleService → TotService → LLMService → ActionService

### Key Components

**Scheduler (`com.tot.scheduler.Scheduler`)**
- Runs every 5 minutes via cron (`tot.scheduler.cron`)
- Triggers asynchronous processing of due schedules

**Tree of Thought System**
- `TotService`: Manages ToT nodes stored as JSON in H2 database
- `TotNode` entities contain decision logic with parent-child relationships
- Each node has content, criteria, and children mapping for decision branches

**External AI Integration**
- `PerplexityService`: REST client to Perplexity API for LLM validation
- `LLMService`: Validates ToT decisions before action execution
- Uses Llama 3.1 Sonar model by default

**Action Execution**
- `ActionService`: Executes validated decisions from ToT evaluation
- `RefinementService`: Updates ToT based on action outcomes for continuous learning

### Data Flow
1. Scheduler triggers at intervals
2. ScheduleService finds due schedules
3. TotService retrieves tree from database as JSON
4. LLMService validates tree logic via external API
5. If valid, ActionService executes the decision
6. RefinementService updates tree based on results

### Configuration
- **Perplexity API**: Key configured in `application.properties` for LLM validation
- **Stock Data API**: Finnhub API integration for real-time stock data validation
- **Scheduler**: Cron expression `0 */5 * * * *` (every 5 minutes)
- **Async Processing**: Enabled via `@EnableAsync` and `AsyncConfig`
- **Database**: MariaDB in production, H2 for development/testing
- **Debug Logging**: Enabled for `com.tot` package for troubleshooting

### Key Technical Details
- **Spring Boot 3.4.3** with Java-based configuration
- **JPA/Hibernate** with automatic schema updates (`ddl-auto=update`)
- **RESTful APIs** with OpenAPI 3.0 documentation
- **Lombok** for boilerplate reduction
- **WebFlux** for reactive programming support
- **Constructor-based dependency injection** throughout

### Testing Framework
- **JUnit 5 (Jupiter)** for unit testing
- **Mockito** for mocking dependencies
- **AssertJ** for fluent assertions
- No test files currently exist - consider adding test coverage