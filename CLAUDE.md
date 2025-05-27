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
```

### Database Access
- H2 Console: http://localhost:8080/h2-console
- Default connection: `jdbc:h2:mem:testdb` (username: `sa`, no password)

### API Documentation
- Swagger UI: http://localhost:8080/swagger-ui.html
- Application runs on port 8080

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
- Perplexity API key in `application.properties` (marked as ****)
- Async processing enabled via `AsyncConfig`
- All services use constructor injection with `@Autowired`