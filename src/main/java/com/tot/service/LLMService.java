package com.tot.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Service for interacting with Language Learning Models using Perplexity API
 */
@Service
public class LLMService {
    private static final Logger logger = LoggerFactory.getLogger(LLMService.class);
    private final PerplexityService perplexityService;

    @Autowired
    public LLMService(PerplexityService perplexityService) {
        this.perplexityService = perplexityService;
        logger.info("LLMService initialized with PerplexityService");
    }

    /**
     * Generate a Tree of Thought structure in JSON format based on the given prompt
     * @param prompt The prompt to generate a tree from
     * @return JSON string representation of the generated tree
     */
    public String generateTreeOfThought(String prompt) {
        logger.info("Generating Tree of Thought from prompt");

        String systemPrompt = """
            You are an expert in creating Tree of Thought (ToT) structures in JSON format.
            
            Based on the user's prompt, create a comprehensive Tree of Thought structure.
            
            The structure should be an array of nodes, where each node has:
            - nodeId: a unique identifier for the node
            - treeId: an identifier for the entire tree
            - content: descriptive content for the node
            - criteria: evaluation criteria for this node
            - children: a mapping of branch keys (e.g., "yes", "no") to child nodeIds
            
            Example format:
            [
              {
                "nodeId": "root",
                "treeId": "decision_tree_1",
                "content": "Should we invest in this project?",
                "criteria": "Evaluate if the project has a positive ROI and aligns with our strategy",
                "children": {
                  "yes": "node_001",
                  "no": "node_002"
                }
              },
              {
                "nodeId": "node_001",
                "treeId": "decision_tree_1",
                "content": "Proceed with investment",
                "criteria": "Allocate budget and resources",
                "children": {}
              },
              {
                "nodeId": "node_002",
                "treeId": "decision_tree_1",
                "content": "Reject investment",
                "criteria": "Document rejection reasons",
                "children": {}
              }
            ]
            
            Return ONLY the JSON array with the tree nodes, without any explanations.
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
        You are an expert in refining Tree of Thought (ToT) structures in JSON format.
        
        You will be given an existing Tree of Thought structure and a new prompt requirement. Your task is to:
        1. Analyze the existing structure while preserving its core logic
        2. Enhance node content with more detailed descriptions based on the new prompt
        3. Improve criteria with more specific evaluation metrics
        4. Add any missing branches or decision paths relevant to the new prompt
        5. Update with the latest relevant information
        6. Ensure all node relationships remain valid
        
        The structure should be an array of nodes, where each node has:
        - nodeId: a unique identifier for the node (preserve existing IDs)
        - treeId: an identifier for the entire tree (preserve existing ID)
        - content: descriptive content for the node (enhance this)
        - criteria: evaluation criteria for this node (enhance this)
        - children: a mapping of branch keys to child nodeIds (expand if needed)
        
        Return the complete refined JSON array with all nodes, without any explanations.
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
                
                Your task: Run through this Tree of Thought and provide both a decision and comprehensive analysis that focuses on changes over the %d-day period.
                
                Instructions:
                1. Analyze the provided Tree of Thought JSON structure
                2. Use your access to real-time information and current data to evaluate each decision criteria
                3. Compare today's data specifically with data from %s
                4. Calculate percentage changes and identify trends over this %d-day period
                5. Walk through the decision tree based on how conditions have evolved
                6. Provide detailed reasoning for each evaluation step with temporal context
                7. Determine if the final outcome represents a positive decision (true) or negative decision (false)
                
                Response format: 
                DECISION: true|false
                CRITERIA: [Provide comprehensive analysis including:
                - Today's current values vs %s values
                - Exact percentage changes over %d days
                - Direction of trend (improving/declining/stable)
                - Momentum analysis (accelerating/decelerating changes)
                - Significant events that occurred during this %d-day period
                - Market sentiment evolution over this timeframe
                - Volume, volatility, and other key metric changes
                - Reasoning for each node evaluation with specific temporal context
                This %d-day comparison data will be used for future tree refinement and trend analysis.]
                
                Always provide specific numbers, exact percentages, and date-based comparisons.
                Focus on the CHANGE and MOMENTUM over exactly %d days, not just current values.
                """, timeframe, comparisonDays, timeframe, comparisonDays, timeframe, comparisonDays, comparisonDays, comparisonDays, comparisonDays);

            String userPrompt = String.format("Run through this TOT and provide decision + detailed criteria analysis comparing today's data with data from %s (%d days ago):\n%s", 
                timeframe, comparisonDays, treeJson);

            logger.debug("Sending {}-day historical comparison validation request to Perplexity", comparisonDays);
            String response = perplexityService.generateCompletionWithSystem(systemPrompt, userPrompt);

            // Parse the response to extract decision and criteria
            return parseValidationResponse(response);
            
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

        return response;
    }
}