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
     * Query the LLM with a specific prompt
     */
    public String query(String prompt) {
        logger.info("Querying LLM with prompt: {}", prompt);

        try {
            String response = this.chatClient.prompt()
                    .user(prompt)
                    .call()
                    .content();

            logger.debug("LLM response: {}", response);
            return response;
        } catch (Exception e) {
            logger.error("Error querying LLM: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to query LLM", e);
        }
    }

    /**
     * Query the LLM with a system prompt and user prompt
     */
    public String query(String systemPrompt, String userPrompt) {
        logger.info("Querying LLM with system prompt and user prompt");
        logger.debug("System prompt: {}", systemPrompt);
        logger.debug("User prompt: {}", userPrompt);

        try {
            String response = this.chatClient.prompt()
                    .system(systemPrompt)
                    .user(userPrompt)
                    .call()
                    .content();

            logger.debug("LLM response: {}", response);
            return response;
        } catch (Exception e) {
            logger.error("Error querying LLM: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to query LLM", e);
        }
    }

    /**
     * Validate a Tree of Thought using the LLM
     */
    public boolean validateTot(String totContent) {
        logger.info("Validating Tree of Thought");

        String systemPrompt = "You are an expert validator for Tree of Thought (ToT) structures. " +
                "Your task is to validate if the provided ToT is structurally sound and logically coherent. " +
                "Check for any inconsistencies, logical errors, or structural problems. " +
                "Respond with VALID if the tree is correctly structured and logical, or INVALID if there are issues.";

        String userPrompt = "Please validate this Tree of Thought structure:\n\n" + totContent;

        String response = query(systemPrompt, userPrompt);

        // Check if the response indicates the ToT is valid
        boolean isValid = response.contains("VALID") && !response.contains("INVALID");

        logger.info("Tree of Thought validation result: {}", isValid ? "VALID" : "INVALID");
        return isValid;
    }

    /**
     * Evaluate a Tree of Thought using the LLM and get a decision
     */
    public String evaluateTot(String totContent, String context) {
        logger.info("Evaluating Tree of Thought with context");

        String systemPrompt = "You are an expert at evaluating Tree of Thought (ToT) decision trees. " +
                "Your task is to traverse the provided tree based on the given context and determine the appropriate path to take. " +
                "Provide your decision as a clear, single-word response that corresponds to one of the branch options in the tree.";

        String userPrompt = "Here is a Tree of Thought to evaluate:\n\n" +
                totContent + "\n\n" +
                "Context for evaluation: " + context + "\n\n" +
                "Based on this ToT and context, what is your decision? " +
                "Please respond with a single word corresponding to one of the branch options.";

        return query(systemPrompt, userPrompt);
    }
}