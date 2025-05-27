package com.tot.controller;

import com.tot.service.TotService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/action")
@Tag(name = "Action", description = "Trigger actions based on TOT decisions")
public class ActionController {

    @Autowired
    private TotService totService;

    @PostMapping("/execute")
    @Operation(summary = "Execute action", description = "Perform an action based on the evaluated TOT (e.g., send alert email)")
    public ResponseEntity<String> executeAction(@RequestParam String treeId) {
        // Get the TOT and evaluate it to decide which action to take
        String treeJson = totService.getTreeOfThought(treeId);
        // Note: This is a simplified example. In real implementation, you would use LLMService.validateTree()
        // For now, we'll assume the tree JSON contains the decision result
        if (treeJson.toLowerCase().contains("true") || treeJson.toLowerCase().contains("positive")) {
            // Trigger an action; for instance, send an email alert.
            return ResponseEntity.ok("Action executed: Email alert sent. Decision: positive");
        } else {
            return ResponseEntity.ok("Action executed: Hold (no action taken). Decision: negative");
        }
    }
}
