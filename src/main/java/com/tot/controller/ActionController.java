package com.tot.controller;

import com.tot.service.LLMService;
import com.tot.service.TotService;
import com.tot.service.LogService;
import com.tot.service.ActionService;
import com.tot.entity.TotLog;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/action")
@Tag(name = "Action", description = "Trigger actions based on TOT decisions")
public class ActionController {

    private static final Logger logger = LoggerFactory.getLogger(ActionController.class);

    private final TotService totService;
    private final LLMService llmService;
    private final LogService logService;
    private final ActionService actionService;

    @Autowired
    public ActionController(TotService totService, LLMService llmService, LogService logService, ActionService actionService) {
        this.totService = totService;
        this.llmService = llmService;
        this.logService = logService;
        this.actionService = actionService;
    }

    @PostMapping("/execute")
    @Operation(summary = "Execute action", description = "Perform an action based on the evaluated TOT (e.g., send alert email)")
    public ResponseEntity<String> executeAction(@RequestParam String treeId) {
        LocalDateTime executionStart = LocalDateTime.now();
        logger.info("Starting action execution for treeId {} at {}", treeId, executionStart);
        
        try {
            // Get the TOT and evaluate it to decide which action to take
            String treeJson = totService.getTreeOfThought(treeId);
            
            if (treeJson == null || treeJson.trim().isEmpty()) {
                logger.error("No tree found for treeId: {}", treeId);
                return ResponseEntity.badRequest().body("Tree not found for ID: " + treeId);
            }

            // Validate the tree with LLMService (this is the potentially long API call)
            String validationResult = llmService.validateTree(treeJson);
            LocalDateTime validationCompleted = LocalDateTime.now();
            
            // Log the LLM response with detailed information
            logger.info("LLM validation completed for treeId {} at {}: result={}", 
                       treeId, validationCompleted, validationResult);
            logger.debug("Full LLM response for treeId {}: {}", treeId, validationResult);
            
            // Store the LLM result and execution details using LogService
            TotLog logEntry = logService.logTreeEvaluation(treeId, treeJson, validationResult);
            logger.info("Stored LLM result in database with log ID: {}", logEntry.getId());

            // Determine action based on validation result
            String actionResult;
            if (validationResult.toLowerCase().contains("true") || validationResult.toLowerCase().contains("positive")) {
                // Trigger positive action
                actionResult = "Action executed: Email alert sent. Decision: positive";
                logger.info("Executing positive action for treeId {}", treeId);
                
                // Here you could call actionService.executeAction() for more complex actions
                // For now, keeping the simple email alert logic
                
            } else {
                // Trigger negative action (or no action)
                actionResult = "Action executed: Hold (no action taken). Decision: negative";
                logger.info("Executing negative action (hold) for treeId {}", treeId);
            }
            
            LocalDateTime executionEnd = LocalDateTime.now();
            logger.info("Action execution completed for treeId {} at {}. Duration: {} ms", 
                       treeId, executionEnd, 
                       java.time.Duration.between(executionStart, executionEnd).toMillis());
            
            return ResponseEntity.ok(actionResult);
            
        } catch (Exception e) {
            LocalDateTime executionEnd = LocalDateTime.now();
            logger.error("Error executing action for treeId {} after {} ms: {}", 
                        treeId, 
                        java.time.Duration.between(executionStart, executionEnd).toMillis(),
                        e.getMessage(), e);
            
            // Log the failure
            try {
                logService.logValidationFailure(treeId);
            } catch (Exception logError) {
                logger.error("Failed to log validation failure: {}", logError.getMessage());
            }
            
            return ResponseEntity.internalServerError()
                    .body("Error executing action for tree " + treeId + ": " + e.getMessage());
        }
    }
}
