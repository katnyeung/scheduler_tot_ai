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
    @Operation(summary = "Execute action", description = "Perform an action based on the evaluated TOT")
    public ResponseEntity<String> executeAction(@RequestParam String treeId) {
        LocalDateTime executionStart = LocalDateTime.now();
        logger.info("Starting action execution for treeId {} at {}", treeId, executionStart);
        
        try {
            // Delegate to ActionService for core processing
            String actionResult = actionService.executeActionForTree(treeId);
            
            LocalDateTime executionEnd = LocalDateTime.now();
            logger.info("Action execution completed for treeId {} at {}. Duration: {} ms", 
                       treeId, executionEnd, 
                       java.time.Duration.between(executionStart, executionEnd).toMillis());
            
            return ResponseEntity.ok(actionResult);
            
        } catch (IllegalArgumentException e) {
            logger.error("Invalid request for treeId {}: {}", treeId, e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
            
        } catch (Exception e) {
            LocalDateTime executionEnd = LocalDateTime.now();
            logger.error("Error executing action for treeId {} after {} ms: {}", 
                        treeId, 
                        java.time.Duration.between(executionStart, executionEnd).toMillis(),
                        e.getMessage(), e);
            
            return ResponseEntity.internalServerError()
                    .body("Error executing action for tree " + treeId + ": " + e.getMessage());
        }
    }
}
