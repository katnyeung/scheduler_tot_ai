package com.tot.controller;

import com.tot.service.LLMService;
import com.tot.service.TotService;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;

@RestController
@RequestMapping("/api/action")
@Tag(name = "Action", description = "Trigger actions based on TOT decisions")
public class ActionController {

    private static final Logger logger = LoggerFactory.getLogger(ActionController.class);

    private final TotService totService;
    private final LLMService llmService;

    @Autowired
    public ActionController(TotService totService, LLMService llmService) {
        this.totService = totService;
        this.llmService = llmService;
    }

    @PostMapping("/execute")
    @Operation(summary = "Execute action", description = "Perform an action based on the evaluated TOT (e.g., send alert email)")
    public ResponseEntity<String> executeAction(@RequestParam String treeId) {
        // Get the TOT and evaluate it to decide which action to take
        String treeJson = totService.getTreeOfThought(treeId);

        // 3. Validate the tree with LLMService (this is the potentially long API call)
        String validationResult = llmService.validateTree(treeJson);
        logger.info("LLM validation completed for treeId {}: result={}", treeId, validationResult);

        // Note: This is a simplified example. In real implementation, you would use LLMService.validateTree()
        // For now, we'll assume the tree JSON contains the decision result
        if (validationResult.toLowerCase().contains("true") || validationResult.toLowerCase().contains("positive")) {
            // Trigger an action; for instance, send an email alert.
            return ResponseEntity.ok("Action executed: Email alert sent. Decision: positive");
        } else {
            return ResponseEntity.ok("Action executed: Hold (no action taken). Decision: negative");
        }
    }
}
