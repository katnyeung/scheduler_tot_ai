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
     * Refine an existing Tree of Thought with the latest data and details
     * @param treeJson The JSON representation of the existing tree
     * @return JSON string representation of the refined tree
     */
    public String refineTreeOfThought(String treeJson) {
        logger.info("Refining existing Tree of Thought");

        String systemPrompt = """
            You are an expert in refining Tree of Thought (ToT) structures in JSON format.
            
            You will be given an existing Tree of Thought structure. Your task is to:
            1. Analyze the existing structure while preserving its core logic
            2. Enhance node content with more detailed descriptions
            3. Improve criteria with more specific evaluation metrics
            4. Add any missing branches or decision paths
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

        String userPrompt = "Here is the existing Tree of Thought to refine:\n" + treeJson;

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
     * Validate a Tree of Thought structure using latest internet data
     * @param treeJson JSON representation of the tree
     * @return "true" if the tree evaluation leads to a positive outcome, "false" otherwise
     */
    public String validateTree(String treeJson) {
        logger.info("Validating Tree of Thought with current data");

        try {
            String systemPrompt = """
                You are evaluating a Tree of Thought decision structure with today's latest information.
                
                Your task: Run through this Tree of Thought and return true or false based on today's data.
                
                Instructions:
                1. Analyze the provided Tree of Thought JSON structure
                2. Use your access to real-time information and current data to evaluate the decision criteria
                3. Walk through the decision tree based on current facts and conditions
                4. Determine if the final outcome represents a positive decision (true) or negative decision (false)
                
                Response format: Return ONLY "true" or "false" - nothing else.
                
                Use current market data, news, trends, and any other relevant real-time information to make your determination.
            """;

            String userPrompt = "Run through this TOT and return true or false for today's data:\n" + treeJson;

            logger.debug("Sending validation request to Perplexity with current data");
            String response = perplexityService.generateCompletionWithSystem(systemPrompt, userPrompt);

            // Clean and normalize response
            String cleanResponse = response.toLowerCase().trim();
            
            // Look for true/false in the response
            if (cleanResponse.contains("true") && !cleanResponse.contains("false")) {
                logger.info("Tree validation result: true");
                return "true";
            } else if (cleanResponse.contains("false") && !cleanResponse.contains("true")) {
                logger.info("Tree validation result: false");
                return "false";
            } else if (cleanResponse.equals("true")) {
                logger.info("Tree validation result: true");
                return "true";
            } else if (cleanResponse.equals("false")) {
                logger.info("Tree validation result: false");
                return "false";
            } else {
                logger.warn("Ambiguous response from LLM: {}. Defaulting to false", response);
                return "false";
            }
        } catch (Exception e) {
            logger.error("Error validating tree: {}", e.getMessage(), e);
            // Don't crash the application, just return false on errors
            return "false";
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