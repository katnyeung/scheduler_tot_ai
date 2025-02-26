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
            assert response != null;
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
}