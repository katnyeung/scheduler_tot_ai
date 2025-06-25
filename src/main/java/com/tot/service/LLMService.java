package com.tot.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.Arrays;

/**
 * Service for interacting with Language Learning Models using Perplexity API
 */
@Service
public class LLMService {
    private static final Logger logger = LoggerFactory.getLogger(LLMService.class);
    private final PerplexityService perplexityService;
    private final StockDataService stockDataService;

    @Autowired
    public LLMService(PerplexityService perplexityService, StockDataService stockDataService) {
        this.perplexityService = perplexityService;
        this.stockDataService = stockDataService;
        logger.info("LLMService initialized with PerplexityService and StockDataService");
    }

    /**
     * Generate a Tree of Thought structure in JSON format based on the given prompt
     * @param prompt The prompt to generate a tree from
     * @return JSON string representation of the generated tree
     */
    public String generateTreeOfThought(String prompt) {
        logger.info("Generating Tree of Thought from prompt");

        String systemPrompt = """
            You are an expert in creating Tree of Thought (ToT) structures in JSON format using binary decision logic.
            
            Based on the user's prompt, create a comprehensive Tree of Thought structure that follows YES/NO decision patterns.
            
            IMPORTANT RULES FOR ToT STRUCTURE:
            1. Each decision node should have binary outcomes: "yes" and "no" branches
            2. Decision criteria should be clear, measurable, and binary-evaluable
            3. Create a logical decision tree that leads to actionable conclusions
            4. Use specific, data-driven criteria whenever possible
            5. End with leaf nodes that represent final actions or decisions
            
            The structure should be an array of nodes, where each node has:
            - nodeId: a unique identifier for the node (e.g., "root", "node_001", "node_002")
            - treeId: an identifier for the entire tree
            - content: descriptive content for the node (the question or decision point)
            - criteria: evaluation criteria for this node (must be yes/no evaluable)
            - children: a mapping of "yes" and "no" to child nodeIds, or empty {} for leaf nodes
            
            Example format:
            [
              {
                "nodeId": "root",
                "treeId": "investment_decision",
                "content": "Should we invest in this stock?",
                "criteria": "Is the current stock price below the 52-week average and showing positive momentum?",
                "children": {
                  "yes": "node_001",
                  "no": "node_002"
                }
              },
              {
                "nodeId": "node_001",
                "treeId": "investment_decision",
                "content": "Check market conditions",
                "criteria": "Is the overall market trend positive with low volatility?",
                "children": {
                  "yes": "node_003",
                  "no": "node_004"
                }
              },
              {
                "nodeId": "node_002",
                "treeId": "investment_decision",
                "content": "Reject investment",
                "criteria": "Stock price is overvalued or showing negative momentum",
                "children": {}
              },
              {
                "nodeId": "node_003",
                "treeId": "investment_decision",
                "content": "Proceed with investment",
                "criteria": "All conditions favorable - invest now",
                "children": {}
              },
              {
                "nodeId": "node_004",
                "treeId": "investment_decision",
                "content": "Wait for better conditions",
                "criteria": "Market conditions unfavorable - postpone investment",
                "children": {}
              }
            ]
            
            Return ONLY the JSON array with the tree nodes, without any explanations or additional text.
        """;

        try {
            String response = perplexityService.generateCompletionWithSystem(systemPrompt, prompt);

            // Clean up response to ensure it's valid JSON
            response = cleanJsonResponse(response);
            logger.debug("Generated tree JSON: {}", response);

            return response;
        } catch (Exception e) {
            logger.error("Error generating tree: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to generate Tree of Thought", e);
        }
    }

    /**
     * Refine an existing Tree of Thought with the latest data and details based on a new prompt
     * @param treeJson The JSON representation of the existing tree
     * @param newPrompt The new prompt requirement to guide refinement
     * @return JSON string representation of the refined tree
     */
    public String refineTreeOfThought(String treeJson, String newPrompt) {
        logger.info("Refining existing Tree of Thought with new prompt");

        String systemPrompt = """
        You are an expert in refining Tree of Thought (ToT) structures in JSON format using binary decision logic.
        
        You will be given an existing Tree of Thought structure and a new prompt requirement. Your task is to:
        1. Analyze the existing structure while preserving its core logic and node relationships
        2. Enhance node content with more detailed descriptions based on the new prompt
        3. Improve criteria to be more specific, measurable, and binary-evaluable (yes/no)
        4. Add any missing decision branches or paths relevant to the new prompt
        5. Update with the latest relevant information and data-driven criteria
        6. Ensure all node relationships remain valid and follow yes/no decision patterns
        7. Preserve existing nodeIds and treeId unless expansion is needed
        
        IMPORTANT REFINEMENT RULES:
        1. Maintain binary decision structure with "yes" and "no" branches
        2. Make criteria more specific and measurable where possible
        3. Ensure each decision point can be clearly evaluated as true/false
        4. Add new nodes only if they enhance the decision logic
        5. Keep the tree focused and avoid unnecessary complexity
        
        The structure should be an array of nodes, where each node has:
        - nodeId: a unique identifier for the node (preserve existing IDs, add new ones if needed)
        - treeId: an identifier for the entire tree (preserve existing ID)
        - content: descriptive content for the node (enhance with more detail)
        - criteria: evaluation criteria for this node (must be yes/no evaluable)
        - children: a mapping of "yes" and "no" to child nodeIds, or empty {} for leaf nodes
        
        Return the complete refined JSON array with all nodes, without any explanations or additional text.
    """;

        String userPrompt = "Here is the existing Tree of Thought to refine:\n" + treeJson + "\n\nNew prompt requirement:\n" + newPrompt;

        try {
            String response = perplexityService.generateCompletionWithSystem(systemPrompt, userPrompt);

            // Clean up response to ensure it's valid JSON
            response = cleanJsonResponse(response);
            logger.debug("Refined tree JSON: {}", response);

            return response;
        } catch (Exception e) {
            logger.error("Error refining tree: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to refine Tree of Thought", e);
        }
    }

    /**
     * Validate a Tree of Thought structure using latest internet data with historical comparison
     * @param treeJson JSON representation of the tree
     * @return ValidationResult containing both decision and detailed criteria
     */
    public ValidationResult validateTreeWithCriteria(String treeJson) {
        logger.info("Validating Tree of Thought with current data and historical comparison");

        try {
            String systemPrompt = """
                You are evaluating a Tree of Thought decision structure with today's latest information compared to historical data.
                
                Your task: Run through this Tree of Thought and provide both a decision and comprehensive analysis that includes historical comparisons.
                
                Instructions:
                1. Analyze the provided Tree of Thought JSON structure
                2. Use your access to real-time information and current data to evaluate each decision criteria
                3. Compare today's data with historical trends (previous days, weeks, months as relevant)
                4. Walk through the decision tree based on current facts, conditions, and how they differ from previous periods
                5. Provide detailed reasoning for each evaluation step with historical context
                6. Determine if the final outcome represents a positive decision (true) or negative decision (false)
                
                Response format: 
                DECISION: true|false
                CRITERIA: [Provide comprehensive analysis including:
                - Current data points (prices, metrics, conditions today)
                - Historical comparison (how today differs from yesterday, last week, last month)
                - Trend analysis (improving, declining, stable patterns)
                - Change percentages and specific differences from previous periods
                - Market sentiment shifts over time
                - Any significant events or changes that occurred recently
                - Reasoning for each node evaluation with temporal context
                This historical comparison data will be used for future tree refinement and trend analysis.]
                
                For investment decisions, include:
                - Current stock prices vs previous day/week/month prices
                - Recent earnings vs historical earnings
                - Market trends and how they've evolved
                - Analyst opinion changes over time
                - Recent news impact vs baseline conditions
                - Volume, volatility, and momentum changes
                
                Always provide specific numbers, percentages, and date-based comparisons where available.
                Focus on what has CHANGED rather than just current static values.
            """;

            String userPrompt = "Run through this TOT and provide decision + detailed criteria analysis comparing today's data with historical trends:\n" + treeJson;

            logger.debug("Sending historical comparison validation request to Perplexity");
            String response = perplexityService.generateCompletionWithSystem(systemPrompt, userPrompt);

            // Parse the response to extract decision and criteria
            return parseValidationResponse(response);
            
        } catch (Exception e) {
            logger.error("Error validating tree with historical criteria: {}", e.getMessage(), e);
            return new ValidationResult("false", "Error during validation: " + e.getMessage());
        }
    }

    /**
     * Validate a Tree of Thought structure with specific historical comparison period
     * @param treeJson JSON representation of the tree
     * @param comparisonDays Number of days back to compare (e.g., 1 for yesterday, 7 for last week)
     * @return ValidationResult containing both decision and detailed criteria
     */
    public ValidationResult validateTreeWithHistoricalComparison(String treeJson, int comparisonDays) {
        logger.info("Validating Tree of Thought with {}-day historical comparison", comparisonDays);

        try {
            String timeframe = switch (comparisonDays) {
                case 1 -> "yesterday";
                case 7 -> "last week (7 days ago)";
                case 30 -> "last month (30 days ago)";
                default -> comparisonDays + " days ago";
            };

            String systemPrompt = String.format("""
                You are evaluating a Tree of Thought decision structure with today's latest information compared to data from %s.
                
                CRITICAL: You MUST systematically walk through the provided Tree of Thought structure node by node. DO NOT perform general self-analysis or commentary. You must follow the tree's decision logic exactly.
                
                Your task: Execute the Tree of Thought step-by-step and provide both a decision and comprehensive analysis that focuses on changes over the %d-day period.
                
                Instructions:
                1. START with the root node of the provided Tree of Thought JSON structure
                2. Evaluate EACH node's specific criteria using real-time information and current data
                3. For each node evaluation, compare today's data specifically with end-of-day data from %s (use closing prices, final daily values, and end-of-day metrics whenever possible)
                4. Based on the criteria evaluation, follow the tree's children mapping to the next node (e.g., if criteria is met, go to "yes" branch, otherwise "no" branch)
                5. Continue traversing through each node until you reach a leaf node (node with no children)
                6. Calculate percentage changes and identify trends over this %d-day period using day-end data points for each node's criteria
                7. The final leaf node determines your decision outcome - provide detailed reasoning for each step of the tree traversal
                
                MANDATORY TREE TRAVERSAL FORMAT:
                Start with: "TREE TRAVERSAL:"
                For each node visited, document:
                - Node ID: [nodeId]
                - Content: [node content]
                - Criteria: [node criteria]
                - Current Data: [relevant current data for evaluation]
                - Historical Data (%s): [relevant historical data]
                - Evaluation Result: [met/not met with reasoning]
                - Next Node: [which child node to follow]
                
                Response format: 
                DECISION: true|false
                CRITERIA: [Provide comprehensive analysis including:
                - Step-by-step tree traversal results (not general analysis)
                - Today's current values vs %s end-of-day values for each evaluated node
                - Exact percentage changes over %d days (using closing/end-of-day data) for each node's criteria
                - Direction of trend (improving/declining/stable) based on day-end comparisons for each decision point
                - Momentum analysis (accelerating/decelerating changes) using daily closing data for each node
                - Significant events that occurred during this %d-day period relevant to each node's criteria
                - Market sentiment evolution over this timeframe using end-of-day indicators for each evaluation
                - Final decision path taken through the tree with supporting data
                This %d-day comparison data will be used for future tree refinement and trend analysis.]
                
                REMEMBER: Follow the tree structure exactly. Do not skip nodes or make assumptions. Evaluate each node's criteria methodically using end-of-day data comparisons.
                """, timeframe, comparisonDays, timeframe, comparisonDays, timeframe, timeframe, comparisonDays, comparisonDays, comparisonDays);

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

            // Parse the response to extract decision and criteria with data source tracking
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
            logger.error("Error validating tree with {}-day historical criteria: {}", comparisonDays, e.getMessage(), e);
            return new ValidationResult("false", "Error during validation: " + e.getMessage());
        }
    }

    /**
     * Legacy method for backward compatibility
     */
    public String validateTree(String treeJson) {
        ValidationResult result = validateTreeWithCriteria(treeJson);
        return result.getResult();
    }

    /**
     * Parse the LLM response to extract decision and criteria
     */
    private ValidationResult parseValidationResponse(String response) {
        try {
            String decision = "false";
            String criteria = response;

            // Look for DECISION: pattern
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

            // Look for CRITERIA: pattern
            if (response.contains("CRITERIA:")) {
                String[] parts = response.split("CRITERIA:", 2);
                if (parts.length > 1) {
                    criteria = parts[1].trim();
                }
            }

            // Fallback: check for simple true/false in response
            if (decision.equals("false") && !response.contains("DECISION:")) {
                String cleanResponse = response.toLowerCase().trim();
                if (cleanResponse.contains("true") && !cleanResponse.contains("false")) {
                    decision = "true";
                } else if (cleanResponse.equals("true")) {
                    decision = "true";
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
     * @param response The raw response from LLM
     * @return Cleaned JSON string
     */
    private String cleanJsonResponse(String response) {
        // Remove markdown code block markers if present
        response = response.replaceAll("```json", "").replaceAll("```", "");

        // Trim whitespace
        response = response.trim();

        // Find JSON array boundaries
        int startIndex = response.indexOf('[');
        int endIndex = response.lastIndexOf(']');
        
        if (startIndex != -1 && endIndex != -1 && endIndex > startIndex) {
            response = response.substring(startIndex, endIndex + 1);
        }

        return response;
    }
}