package com.tot.controller;

import com.tot.service.ActionService;
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

    private final ActionService actionService;

    @Autowired
    public ActionController(ActionService actionService) {
        this.actionService = actionService;
    }

    @PostMapping("/execute")
    @Operation(summary = "Execute action with historical comparison", 
               description = "Perform an action based on the evaluated TOT with specific historical comparison period")
    public ResponseEntity<String> executeAction(
            @RequestParam String treeId, 
            @RequestParam(defaultValue = "1") int comparisonDays) {
        LocalDateTime executionStart = LocalDateTime.now();
        logger.info("Starting action execution for treeId {} with {}-day historical comparison at {}", 
                   treeId, comparisonDays, executionStart);
        
        // Validate comparisonDays parameter
        if (comparisonDays < 1 || comparisonDays > 365) {
            logger.error("Invalid comparisonDays parameter: {}. Must be between 1 and 365.", comparisonDays);
            return ResponseEntity.badRequest().body("comparisonDays must be between 1 and 365");
        }
        
        try {
            // Delegate to ActionService for core processing with historical comparison (service selected by feature flag)
            String actionResult = actionService.executeActionForTreeWithHistoricalComparison(treeId, comparisonDays);
            
            LocalDateTime executionEnd = LocalDateTime.now();
            logger.info("Action execution with {}-day comparison completed for treeId {} at {}. Duration: {} ms", 
                       comparisonDays, treeId, executionEnd, 
                       java.time.Duration.between(executionStart, executionEnd).toMillis());
            
            return ResponseEntity.ok(actionResult);
            
        } catch (IllegalArgumentException e) {
            logger.error("Invalid request for treeId {} with {}-day comparison: {}", treeId, comparisonDays, e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
            
        } catch (Exception e) {
            LocalDateTime executionEnd = LocalDateTime.now();
            logger.error("Error executing action for treeId {} with {}-day comparison after {} ms: {}", 
                        treeId, comparisonDays,
                        java.time.Duration.between(executionStart, executionEnd).toMillis(),
                        e.getMessage(), e);
            
            return ResponseEntity.internalServerError()
                    .body("Error executing action for tree " + treeId + " with " + comparisonDays + "-day comparison: " + e.getMessage());
        }
    }
}
