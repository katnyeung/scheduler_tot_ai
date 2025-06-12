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

1. **Scheduler** triggers at configured intervals (every 5 minutes via cron)
2. **ScheduleService** delegates async processing to ActionService
3. **ActionService** finds due schedules and processes each asynchronously:
   - Retrieves ToT tree structure from **TotService** (stored in H2 database as JSON)
   - Validates tree logic using **LLMService** with historical comparison (1-365 days back)
   - **LLMService** calls **PerplexityService** (Perplexity API) for AI validation
   - **LogService** records validation results and criteria in TotLog entities
   - If validation passes, executes the action logic
   - **LogService** captures final execution outcomes
   - Updates schedule status (IN_PROGRESS → COMPLETED/ERROR)
4. **RefinementService** (future) will analyze logged results for continuous improvement

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

## Future Enhancement: Darwin Gödel Machine Inspired Self-Refinement

Based on research into Darwin Gödel Machine (https://arxiv.org/abs/2505.22954), the system can be enhanced with autonomous self-improvement capabilities using execution logs for ToT refinement.

### Proposed Self-Refinement Mechanisms

#### 1. ToT Archive System
- **Concept**: Maintain an archive of successful ToT tree configurations
- **Implementation**: Store high-performing tree structures as templates for generating variations
- **Data Source**: `TotLog` entries with high validation success and action completion rates

#### 2. Execution-Based Tree Evolution
- **Performance Analysis**: Analyze patterns in `TotLog.validationResult` and action outcomes
- **Tree Optimization**: Identify which tree structures consistently produce better results
- **Criteria Refinement**: Automatically adjust validation criteria based on historical performance

#### 3. Multi-Path Tree Exploration
- **Parallel Evaluation**: Run multiple ToT variants simultaneously for each schedule
- **Tournament Selection**: Compare results and favor winning tree architectures
- **Branching Strategy**: Evolve tree structure based on execution success patterns

#### 4. Context-Aware Tree Selection
- **Pattern Recognition**: Learn which tree types perform best in specific contexts
- **Temporal Adaptation**: Adjust tree selection based on time patterns, previous failures, or environmental conditions
- **Dynamic Switching**: Automatically select optimal tree templates based on execution context

### Implementation Examples

#### Tree Performance Metrics
```
Example Analysis:
- Tree A: 80% validation success, 30% action success → Archive but deprioritize
- Tree B: 60% validation success, 90% action success → Archive as high priority template
- Tree C: 95% validation success, 10% action success → Analyze for over-optimization
```

#### Criteria Evolution Pattern
```
Historical Pattern Discovery:
- "time < 10am" criterion → 85% action success
- "weather = sunny" criterion → 90% action success  
- "previous_action = failed" → Switch to conservative tree variant

Auto-Refinement: Boost successful criteria weights, introduce context-switching logic
```

#### Validation Strategy Evolution
```
Initial: "Is action feasible?" → 80% pass, 40% execute successfully
Evolved: "Is action feasible AND similar actions succeeded recently?" → 60% pass, 85% execute successfully
```

### Data Requirements for Implementation
- Enhanced `TotLog` tracking with action outcome correlation
- Tree performance metrics storage
- Context metadata capture (time, previous results, environmental factors)
- Archive system for successful tree configurations

### Benefits
- **Autonomous Improvement**: System learns from its own execution history
- **Reduced Manual Tuning**: Automatic optimization of tree structures and validation criteria
- **Adaptive Performance**: Better decision-making through accumulated experience
- **Continuous Evolution**: Ongoing refinement without human intervention

This approach transforms the current ToT system from static tree evaluation to a continuously learning and self-improving decision framework.

