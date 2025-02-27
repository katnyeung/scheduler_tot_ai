package com.tot.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Service for interacting with Language Learning Models
 */
@Service
public class LLMService {
    private static final Logger logger = LoggerFactory.getLogger(LLMService.class);
    private final ChatClient chatClient;

    @Autowired
    public LLMService(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
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
            String response = this.chatClient.prompt()
                    .system(systemPrompt)
                    .user(prompt)
                    .call()
                    .content();

            // Clean up response to ensure it's valid JSON
            response = cleanJsonResponse(response);
            logger.debug("Generated tree JSON: {}", response);

            return response;
        } catch (Exception e) {
            logger.error("Error generating tree: {}", e.getMessage());
            throw new RuntimeException("Failed to generate Tree of Thought", e);
        }
    }

    /**
     * Validate a Tree of Thought structure
     * @param treeJson JSON representation of the tree
     * @return "VALID" if the tree is valid, "INVALID" otherwise
     */
    public String validateTree(String treeJson) {
        logger.info("Validating Tree of Thought");

        String systemPrompt = """
            You are an expert validator for Tree of Thought (ToT) structures.
            
            Analyze the provided JSON tree structure and determine if it's valid.
            
            Check for proper node relationships, logical flow, and completeness.
            
            Respond with VALID if the tree is correct, or INVALID if there are issues.
            
            Follow these steps:
                    1. Start at the root node of the tree
                    2. At each node, evaluate the criteria based on the context
                    3. Follow the appropriate branch based on your evaluation
                    4. Continue until you reach a terminal node
                    5. Return your path and final decision
                    Format your response in VALID / INVALID
        """;

        String userPrompt = "Tree of Thought to validate:\n" + treeJson;

        try {
            String response = this.chatClient.prompt()
                    .system(systemPrompt)
                    .user(userPrompt)
                    .call()
                    .content();

            // Extract VALID or INVALID from the response
            if (response.contains("VALID") && !response.contains("INVALID")) {
                return "VALID";
            } else {
                return "INVALID";
            }
        } catch (Exception e) {
            logger.error("Error validating tree: {}", e.getMessage());
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