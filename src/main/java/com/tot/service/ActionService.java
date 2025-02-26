package com.tot.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.tot.entity.Action;
import com.tot.repository.ActionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Service for executing actions based on ToT decisions
 */
@Service
public class ActionService {
    private static final Logger logger = LoggerFactory.getLogger(ActionService.class);
    private final ActionRepository actionRepository;
    private final ObjectMapper objectMapper;

    @Autowired
    public ActionService(ActionRepository actionRepository) {
        this.actionRepository = actionRepository;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Execute an action with context
     * @param action The action to execute
     * @param context Additional context for the action
     */
    @Transactional
    public void executeAction(Action action, ObjectNode context) {
        logger.info("Executing action: {}, type: {}", action.getId(), action.getActionType());

        try {
            // Parse action data
            JsonNode actionData = objectMapper.readTree(action.getActionData());

            // Execute based on action type
            switch (action.getActionType()) {
                case "EMAIL_ALERT" -> sendEmailAlert(actionData, context);
                case "API_CALL" -> makeApiCall(actionData, context);
                case "REFINE_TREE" -> refineTree(actionData, context);
                default -> {
                    logger.warn("Unknown action type: {}", action.getActionType());
                    throw new IllegalArgumentException("Unsupported action type: " + action.getActionType());
                }
            }

            // Record execution
            recordActionExecution(action, context);

        } catch (Exception e) {
            logger.error("Error executing action: {}", e.getMessage());
            throw new RuntimeException("Failed to execute action", e);
        }
    }

    // Action type implementations

    private void sendEmailAlert(JsonNode actionData, ObjectNode context) {
        String recipient = actionData.path("recipient").asText("default@example.com");
        String subject = actionData.path("subject").asText("ToT Alert");

        logger.info("Sending email alert to: {}, subject: {}", recipient, subject);

        // Implementation would go here
    }

    private void makeApiCall(JsonNode actionData, ObjectNode context) {
        String url = actionData.path("url").asText();
        String method = actionData.path("method").asText("GET");

        logger.info("Making API call to: {}, method: {}", url, method);

        // Implementation would go here
    }

    private void refineTree(JsonNode actionData, ObjectNode context) {
        String treeId = actionData.path("treeId").asText();
        if (treeId.isEmpty() && context.has("treeId")) {
            treeId = context.path("treeId").asText();
        }

        logger.info("Refining tree: {}", treeId);

        // Implementation would go here
    }

    // Helper methods

    private void recordActionExecution(Action action, JsonNode context) throws JsonProcessingException {
        // Update action with execution record
        JsonNode actionData = objectMapper.readTree(action.getActionData());
        ((ObjectNode) actionData).put("lastExecuted", LocalDateTime.now().toString());

        action.setActionData(objectMapper.writeValueAsString(actionData));
        actionRepository.save(action);
    }
}