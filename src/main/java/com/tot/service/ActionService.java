package com.tot.service;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.tot.entity.Action;
import com.tot.repository.ActionRepository;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class ActionService {

    private static final Logger logger = LoggerFactory.getLogger(ActionService.class);

    @Autowired
    private ActionRepository actionRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Create a new action with specified type and data
     */
    @Transactional
    public Action createAction(String actionType, Object actionDataObject) throws JsonProcessingException {
        Action action = new Action();
        action.setId(java.util.UUID.randomUUID().toString());
        action.setActionType(actionType);
        action.setActionData(objectMapper.writeValueAsString(actionDataObject));
        action.setCreatedAt(LocalDateTime.now());

        return actionRepository.save(action);
    }

    /**
     * Update an existing action
     */
    @Transactional
    public Action updateAction(Action action) {
        return actionRepository.save(action);
    }

    /**
     * Parse action data into a specified type
     */
    public <T> T parseActionData(Action action, Class<T> valueType) throws JsonProcessingException {
        return objectMapper.readValue(action.getActionData(), valueType);
    }

    /**
     * Get actions by type
     */
    public List<Action> getActionsByType(String actionType) {
        return actionRepository.findByActionType(actionType);
    }

    /**
     * Find an action by ID
     */
    public Optional<Action> findById(String id) {
        return actionRepository.findById(id);
    }

    /**
     * Get all actions created between two dates
     */
    public List<Action> getActionsBetweenDates(LocalDateTime start, LocalDateTime end) {
        return actionRepository.findByCreatedAtBetween(start, end);
    }

    /**
     * Execute an action with a given context
     */
    @Transactional
    public void executeAction(Action action, JsonNode context) throws JsonProcessingException {
        logger.info("Executing action {} of type {}", action.getId(), action.getActionType());

        // Parse the action data
        JsonNode actionData = objectMapper.readTree(action.getActionData());

        // Different execution logic based on action type
        switch (action.getActionType()) {
            case "EMAIL_ALERT" -> executeEmailAlert(actionData, context);
            case "API_CALL" -> executeApiCall(actionData, context);
            case "DATABASE_UPDATE" -> executeDatabaseUpdate(actionData, context);
            case "LLM_PROMPT" -> executeLlmPrompt(actionData, context);
            case "REFINE_TREE" -> executeTreeRefinement(actionData, context);
            default -> {
                logger.warn("Unknown action type: {}", action.getActionType());
                throw new IllegalArgumentException("Unsupported action type: " + action.getActionType());
            }
        }

        // Record action execution
        recordActionExecution(action, context);
    }

    /**
     * Record that an action was executed
     */
    private void recordActionExecution(Action action, JsonNode context) throws JsonProcessingException {
        // Parse the current action data
        JsonNode actionData = objectMapper.readTree(action.getActionData());

        // Add execution record
        if (!actionData.has("executions")) {
            ((ObjectNode) actionData).putArray("executions");
        }

        ObjectNode execution = objectMapper.createObjectNode();
        execution.put("timestamp", LocalDateTime.now().toString());
        execution.set("context", context);

        // Add to the executions array
        ((ObjectNode) actionData).withArray("executions").add(execution);

        // Update the action
        action.setActionData(objectMapper.writeValueAsString(actionData));
        actionRepository.save(action);
    }

    /**
     * Execute email alert action
     */
    private void executeEmailAlert(JsonNode actionData, JsonNode context) {
        String recipient = actionData.path("recipient").asText("default@example.com");
        String subject = actionData.path("subject").asText("ToT Alert");
        String template = actionData.path("template").asText("No template provided");

        // Replace placeholders in template with context values
        if (context != null) {
            // Simple placeholder replacement
            for (Iterator<Map.Entry<String, JsonNode>> it = context.fields(); it.hasNext(); ) {
                var field = it.next();
                String placeholder = "{" + field.getKey() + "}";
                template = template.replace(placeholder, field.getValue().asText());
            }
        }

        // In a real implementation, you would send an actual email here
        logger.info("EMAIL ALERT - To: {}, Subject: {}, Body: {}", recipient, subject, template);
    }

    /**
     * Execute API call action
     */
    private void executeApiCall(JsonNode actionData, JsonNode context) {
        String url = actionData.path("url").asText();
        String method = actionData.path("method").asText("GET");
        JsonNode payload = actionData.path("payload");

        // In a real implementation, you would make an HTTP request here
        logger.info("API CALL - URL: {}, Method: {}, Payload: {}, Context: {}",
                url, method, payload, context);
    }

    /**
     * Execute database update action
     */
    private void executeDatabaseUpdate(JsonNode actionData, JsonNode context) {
        String entity = actionData.path("entity").asText();
        String operation = actionData.path("operation").asText();
        JsonNode data = actionData.path("data");

        // In a real implementation, you would perform a database operation
        logger.info("DATABASE UPDATE - Entity: {}, Operation: {}, Data: {}, Context: {}",
                entity, operation, data, context);
    }

    /**
     * Execute LLM prompt action
     */
    private void executeLlmPrompt(JsonNode actionData, JsonNode context) {
        String prompt = actionData.path("prompt").asText();

        // Replace placeholders in the prompt with context values
        if (context != null) {
            for (Iterator<Map.Entry<String, JsonNode>> it = context.fields(); it.hasNext(); ) {
                var field = it.next();
                String placeholder = "{" + field.getKey() + "}";
                prompt = prompt.replace(placeholder, field.getValue().asText());
            }
        }

        // In a real implementation, you would call the LLM here
        logger.info("LLM PROMPT - Prompt: {}, Context: {}", prompt, context);
    }

    /**
     * Execute tree refinement action
     */
    private void executeTreeRefinement(JsonNode actionData, JsonNode context) {
        String treeId = actionData.path("treeId").asText();
        if (treeId.isEmpty() && context.has("treeId")) {
            treeId = context.path("treeId").asText();
        }

        // In a real implementation, you would integrate with TotService here
        logger.info("TREE REFINEMENT - TreeId: {}, Context: {}", treeId, context);
    }

    /**
     * Create a standard email alert action
     */
    public Action createEmailAlertAction(String recipient, String subject, String template) throws JsonProcessingException {
        ObjectNode actionData = objectMapper.createObjectNode();
        actionData.put("recipient", recipient);
        actionData.put("subject", subject);
        actionData.put("template", template);

        return createAction("EMAIL_ALERT", actionData);
    }

    /**
     * Create a standard API call action
     */
    public Action createApiCallAction(String url, String method, Object payload) throws JsonProcessingException {
        ObjectNode actionData = objectMapper.createObjectNode();
        actionData.put("url", url);
        actionData.put("method", method);
        actionData.set("payload", objectMapper.valueToTree(payload));

        return createAction("API_CALL", actionData);
    }

    /**
     * Create a standard LLM prompt action
     */
    public Action createLlmPromptAction(String prompt) throws JsonProcessingException {
        ObjectNode actionData = objectMapper.createObjectNode();
        actionData.put("prompt", prompt);

        return createAction("LLM_PROMPT", actionData);
    }

    /**
     * Create a tree refinement action
     */
    public Action createTreeRefinementAction(String treeId) throws JsonProcessingException {
        ObjectNode actionData = objectMapper.createObjectNode();
        actionData.put("treeId", treeId);

        return createAction("REFINE_TREE", actionData);
    }
}