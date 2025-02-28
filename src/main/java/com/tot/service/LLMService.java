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
     * Validate a Tree of Thought structure by walking through the decision path
     * @param treeJson JSON representation of the tree
     * @return "VALID" if the tree leads to a valid decision path, "INVALID" otherwise
     */
    public String validateTree(String treeJson) {
        logger.info("Validating Tree of Thought by traversal");

        try {
            String systemPrompt = """
                You are an expert evaluator walking through a Tree of Thought (ToT) decision structure.
                
                I'll provide you with a JSON representation of a decision tree. Your task is to:
                
                1. Identify the root node of the tree
                2. At each node, carefully read:
                   - content: what this decision point is about
                   - criteria: the evaluation criteria for making a decision
                
                3. For each node, evaluate the criteria based on current information and facts
                   - Use your knowledge and access to information to determine if the criteria are met
                   - Make a decision: "yes" if criteria are met, "no" if not
                
                4. Based on your decision, follow the appropriate branch in the "children" object
                   - If you chose "yes", follow the nodeId mapped to the "yes" key
                   - If you chose "no", follow the nodeId mapped to the "no" key
                
                5. Continue this process, traversing from node to node until you:
                   - Reach a terminal node (a node with empty children)
                   - OR encounter an invalid path (reference to non-existent node)
                   - OR encounter an ambiguous decision (unclear criteria)
                
                6. Document your path through the tree, showing:
                   - Each node you visited
                   - The decision you made at each node
                   - The reasoning behind each decision
                
                7. Final verdict:
                   - If you reached a terminal node through valid decisions: respond with "VALID"
                   - If you encountered any issues (invalid references, ambiguous criteria, circular paths, etc.): respond with "INVALID"
                
                Your evaluation should be based on factual information and sound reasoning.
            """;

            String userPrompt = "Tree of Thought to evaluate by traversal:\n" + treeJson;

            logger.debug("Sending traversal validation request to Perplexity");
            String response = perplexityService.generateCompletionWithSystem(systemPrompt, userPrompt);

            // Check if the response contains the validation result
            String lowercaseResponse = response.toLowerCase();

            // Look for the final verdict
            if (lowercaseResponse.contains("verdict: valid") ||
                    lowercaseResponse.contains("final verdict: valid") ||
                    (lowercaseResponse.contains("valid") && !lowercaseResponse.contains("invalid"))) {
                logger.info("Tree validated as VALID after traversal");
                return "VALID";
            } else {
                logger.info("Tree validated as INVALID after traversal");
                return "INVALID";
            }
        } catch (Exception e) {
            logger.error("Error traversing tree: {}", e.getMessage(), e);
            // Don't crash the application, just return INVALID on errors
            return "INVALID";
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