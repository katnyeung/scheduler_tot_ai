package com.tot.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import java.util.Arrays;

/**
 * Stock-specific implementation of LLMService for financial decision trees
 * Focuses on BUY/SELL/HOLD decisions with percentage-based recommendations
 * Activated when tot.llm.service=stock
 */
@Service
@ConditionalOnProperty(name = "tot.llm.service", havingValue = "stock")
public class StockLLMServiceImpl implements LLMService {
    private static final Logger logger = LoggerFactory.getLogger(StockLLMServiceImpl.class);
    
    private final PerplexityService perplexityService;
    private final StockDataService stockDataService;

    @Autowired
    public StockLLMServiceImpl(PerplexityService perplexityService, StockDataService stockDataService) {
        this.perplexityService = perplexityService;
        this.stockDataService = stockDataService;
        logger.info("StockLLMServiceImpl initialized with PerplexityService and StockDataService");
    }

    @Override
    public String generateTreeOfThought(String prompt) {
        logger.info("Generating Stock Tree of Thought from prompt");

        String systemPrompt = """
            You are an expert in creating Stock Trading Tree of Thought (ToT) structures in JSON format using binary decision logic.
            
            Based on the user's prompt, create a comprehensive Tree of Thought structure that follows YES/NO decision patterns for stock trading decisions.
            
            IMPORTANT RULES FOR STOCK ToT STRUCTURE:
            1. Each decision node should have binary outcomes: "yes" and "no" branches
            2. Decision criteria should be clear, measurable, and binary-evaluable for stock analysis
            3. Create a logical decision tree that leads to BUY/SELL/HOLD conclusions
            4. Use specific, data-driven criteria focused on stock metrics (price, volume, ratios, sentiment, etc.)
            5. End with leaf nodes that represent final trading actions with percentage-based recommendations
            
            IMPORTANT: For terminal nodes (leaf nodes with empty children), the criteria field must include:
            - Action recommendation: BUY, SELL, or HOLD
            - Percentage confidence: 40-95% (e.g., "BUY 75%", "HOLD 60%", "SELL 85%")
            - Brief reasoning after the percentage
            
            Terminal node criteria format: "[ACTION] [PERCENTAGE]% - [reasoning]"
            Examples:
            - "BUY 80% - Strong fundamentals and positive momentum indicators"
            - "HOLD 65% - Mixed signals require cautious monitoring"
            - "SELL 90% - Multiple risk factors and negative outlook"
            
            Return ONLY the JSON array with the tree nodes, without any explanations or additional text.
        """;

        try {
            String response = perplexityService.generateCompletionWithSystem(systemPrompt, prompt);
            response = cleanJsonResponse(response);
            logger.debug("Generated stock tree JSON: {}", response);
            return response;
        } catch (Exception e) {
            logger.error("Error generating stock tree: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to generate Stock Tree of Thought", e);
        }
    }

    @Override
    public String refineTreeOfThought(String treeJson, String newPrompt) {
        logger.info("Refining existing Stock Tree of Thought with new prompt");

        String systemPrompt = """
        You are an expert in refining Stock Trading Tree of Thought (ToT) structures in JSON format using binary decision logic.
        
        Enhance the existing tree with stock-specific improvements, ensuring terminal nodes include percentage-based recommendations (BUY/SELL/HOLD X%).
        
        IMPORTANT: For terminal nodes (leaf nodes with empty children), ensure the criteria field includes:
        - Action recommendation: BUY, SELL, or HOLD
        - Percentage confidence: 40-95% (e.g., "BUY 75%", "HOLD 60%", "SELL 85%")
        - Brief reasoning after the percentage
        
        Return the complete refined JSON array with all nodes, without any explanations or additional text.
    """;

        String userPrompt = "Here is the existing Tree of Thought to refine:\n" + treeJson + "\n\nNew prompt requirement:\n" + newPrompt;

        try {
            String response = perplexityService.generateCompletionWithSystem(systemPrompt, userPrompt);
            response = cleanJsonResponse(response);
            logger.debug("Refined stock tree JSON: {}", response);
            return response;
        } catch (Exception e) {
            logger.error("Error refining stock tree: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to refine Stock Tree of Thought", e);
        }
    }

    @Override
    public ValidationResult validateTreeWithHistoricalComparison(String treeJson, int comparisonDays) {
        logger.info("Validating Stock Tree of Thought with {}-day historical comparison", comparisonDays);

        try {
            String timeframe = switch (comparisonDays) {
                case 1 -> "yesterday";
                case 7 -> "last week (7 days ago)";
                case 30 -> "last month (30 days ago)";
                default -> comparisonDays + " days ago";
            };

            String systemPrompt = String.format("""
                You are evaluating a Stock Trading Tree of Thought decision structure with today's latest stock market information compared to data from %s.
                
                CRITICAL: You MUST systematically walk through the provided Tree of Thought structure node by node.
                
                Follow the tree's decision logic exactly and provide both a decision and comprehensive stock analysis focusing on changes over the %d-day period.
                
                Response format: 
                DECISION: true|false
                CRITERIA: [Provide comprehensive stock analysis with step-by-step tree traversal results and %d-day stock comparison data]
                
                REMEMBER: Follow the stock tree structure exactly. Evaluate each node's criteria methodically using end-of-day stock data comparisons.
                """, timeframe, comparisonDays, comparisonDays);

            String userPrompt = String.format("Run through this TOT and provide decision + detailed criteria analysis comparing today's data with data from %s (%d days ago):\n%s", 
                timeframe, comparisonDays, treeJson);

            // Enrich prompt with real stock data if stock criteria detected
            boolean stockDataEnriched = false;
            if (stockDataService.containsStockCriteria(treeJson)) {
                logger.info("Stock criteria detected, enriching prompt with real market data");
                userPrompt = stockDataService.enrichPromptWithStockData(userPrompt, treeJson, comparisonDays);
                stockDataEnriched = true;
            }

            logger.debug("Sending {}-day historical comparison validation request to Perplexity (stock enriched: {})", 
                comparisonDays, stockDataEnriched);
            String response = perplexityService.generateCompletionWithSystemAndWebSearch(systemPrompt, userPrompt, comparisonDays);

            ValidationResult result = parseValidationResponse(response);
            
            // Create enhanced ValidationResult with stock data tracking
            if (stockDataEnriched) {
                return new ValidationResult(result.getResult(), result.getCriteria(), 
                    Arrays.asList("Yahoo Finance API", "Perplexity Web Search"), null, true);
            } else {
                return new ValidationResult(result.getResult(), result.getCriteria(), 
                    Arrays.asList("Perplexity Web Search"), null, false);
            }
            
        } catch (Exception e) {
            logger.error("Error validating stock tree with {}-day historical criteria: {}", comparisonDays, e.getMessage(), e);
            return new ValidationResult("false", "Error during validation: " + e.getMessage());
        }
    }

    @Override
    public String validateTree(String treeJson) {
        ValidationResult result = validateTreeWithHistoricalComparison(treeJson, 1);
        return result.getResult();
    }

    /**
     * Parse the LLM response to extract decision and criteria
     */
    private ValidationResult parseValidationResponse(String response) {
        try {
            String decision = "false";
            String criteria = response;

            if (response.contains("DECISION:")) {
                String[] parts = response.split("DECISION:", 2);
                if (parts.length > 1) {
                    String decisionPart = parts[1].trim();
                    if (decisionPart.toLowerCase().startsWith("true")) {
                        decision = "true";
                    } else if (decisionPart.toLowerCase().startsWith("false")) {
                        decision = "false";
                    }
                }
            }

            if (response.contains("CRITERIA:")) {
                String[] parts = response.split("CRITERIA:", 2);
                if (parts.length > 1) {
                    criteria = parts[1].trim();
                }
            }

            logger.info("Parsed validation - Decision: {}, Criteria length: {}", decision, criteria.length());
            return new ValidationResult(decision, criteria);
            
        } catch (Exception e) {
            logger.error("Error parsing validation response: {}", e.getMessage());
            return new ValidationResult("false", "Error parsing response: " + response);
        }
    }

    /**
     * Clean up a JSON response from LLM to ensure it's valid
     */
    private String cleanJsonResponse(String response) {
        response = response.replaceAll("```json", "").replaceAll("```", "");
        response = response.trim();

        int startIndex = response.indexOf('[');
        int endIndex = response.lastIndexOf(']');
        
        if (startIndex != -1 && endIndex != -1 && endIndex > startIndex) {
            response = response.substring(startIndex, endIndex + 1);
        }

        return response;
    }
}