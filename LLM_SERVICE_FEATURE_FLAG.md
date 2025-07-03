# LLM Service Feature Flag System

## Overview

The system now uses a simple feature flag approach to switch between different LLM service implementations. This is much cleaner than the previous complex architecture.

## How It Works

### 1. Feature Flag Configuration
In `application.properties`:
```properties
# LLM Service Configuration - Feature Flag
# Options: "donothing" (default), "stock", "crypto", "business", etc.
tot.llm.service=donothing
```

### 2. Service Implementations

**LLMService** - Interface with 4 methods (generateTreeOfThought, refineTreeOfThought, validateTreeWithHistoricalComparison, validateTree)
**LLMServiceDoNothing** - Default mock implementation (activated when `tot.llm.service=donothing` or not set)
**StockLLMServiceImpl** - Stock-specific implementation (activated when `tot.llm.service=stock`)

### 3. Automatic Service Selection

Spring automatically wires the correct implementation based on the feature flag:

```java
@Service
@ConditionalOnProperty(name = "tot.llm.service", havingValue = "donothing", matchIfMissing = true)
public class LLMServiceDoNothing implements LLMService { ... }

@Service  
@ConditionalOnProperty(name = "tot.llm.service", havingValue = "stock")
public class StockLLMServiceImpl implements LLMService { ... }
```

### 4. Usage

All existing code works unchanged:
```java
@Service
public class ActionService {
    private final LLMService llmService;
    
    // Spring automatically injects the correct implementation based on feature flag
    public ActionService(LLMService llmService) { 
        this.llmService = llmService;
    }
    
    public ValidationResult processTree(String treeJson) {
        return llmService.validateTreeWithHistoricalComparison(treeJson, 1);
    }
}
```

## Switching Services

To switch from mock to stock service:

1. **Change application.properties:**
```properties
tot.llm.service=stock
```

2. **Restart application** - Spring will automatically wire `StockLLMServiceImpl`

3. **All APIs work the same** - no code changes needed

## Adding New Services

To add a new service (e.g., CryptoLLMService):

1. **Create implementation:**
```java
@Service
@ConditionalOnProperty(name = "tot.llm.service", havingValue = "crypto")
public class CryptoLLMServiceImpl implements LLMService {
    // Crypto-specific logic with "BUY 80%", "HODL 60%", "SELL 90%" 
}
```

2. **Update application.properties:**
```properties
tot.llm.service=crypto
```

3. **Done!** - Spring handles the rest automatically

## Current Configurations

### DoNothing Service (Default)
```properties
tot.llm.service=donothing
```
- Returns mock trees
- No external API calls
- Good for testing/development

### Stock Service  
```properties
tot.llm.service=stock
```
- Calls Perplexity API
- Stock data integration
- Percentage-based recommendations: "BUY 75%", "HOLD 60%", "SELL 85%"

## Benefits

✅ **Simple** - Just one property change
✅ **Clean** - No complex dependency injection
✅ **Extensible** - Easy to add new services  
✅ **Backward Compatible** - Existing code unchanged
✅ **Spring Native** - Uses Spring's conditional beans

This is much cleaner than the previous over-engineered approach!