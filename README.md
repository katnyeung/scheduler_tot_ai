# Tree of Thought Scheduling System

A Spring Boot application that implements autonomous task execution using Tree of Thought (ToT) reasoning with AI integration.

## Overview

This system provides:
- **Scheduling**: Automated task execution at specified intervals
- **Tree of Thought (ToT)**: Hierarchical decision-making framework for complex task reasoning
- **AI Integration**: LLM validation through Perplexity API
- **Action Execution**: Task execution based on validated ToT decisions
- **Continuous Learning**: System refinement based on execution outcomes

## Architecture

The following diagram illustrates the relationship between the Scheduler, Tree of Thought (ToT), Action, and Refinement:

![Untitled diagram-2025-02-17-002123](https://github.com/user-attachments/assets/4e75e6d1-a5c2-4d6a-ac0c-456c4482eb0f)

### Core Components

- **Scheduler**: Executes scheduled tasks at 5-minute intervals via cron expression
- **ScheduleService**: Delegates schedule processing to ActionService
- **ActionService**: Core service containing ToT processing logic and action execution
- **TotService**: Handles Tree of Thought node management and JSON serialization
- **LLMService**: Validates ToT decisions using external AI services
- **LogService**: Records execution results, validation outcomes, and system events for analysis

### Data Flow

1. Scheduler triggers at configured intervals
2. ScheduleService delegates processing to ActionService
3. ActionService identifies due schedules
4. TotService retrieves tree structure from H2 database
5. LLMService validates tree logic via Perplexity API
6. LogService records tree evaluation and validation results
7. ActionService executes core logic for validated decisions
8. LogService captures execution outcomes and results

## Technical Specifications

### Database
- **H2 In-Memory Database**: Stores ToT nodes, schedules, actions, and execution logs
- **Log Storage**: TotLog entities capture execution results for refinement analysis
- **Console Access**: Available at `http://localhost:8080/h2-console`
- **Connection**: `jdbc:h2:mem:testdb` (username: `sa`, no password)

### API Documentation
- **Swagger UI**: Available at `http://localhost:8080/swagger-ui.html`
- **Port**: Application runs on port 8080

### Build and Deployment
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

### Configuration
- Perplexity API integration for LLM services
- Async processing enabled for concurrent task execution
- Cron-based scheduling with configurable intervals

