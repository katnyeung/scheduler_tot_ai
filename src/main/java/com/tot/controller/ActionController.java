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
    public ResponseEntity<String> executeAction(@RequestParam String totData) {
        // Evaluate the TOT to decide which action to take using Spring AI.
        String evaluationResult = totService.getTreeOfThought(totData);
        if ("Good".equalsIgnoreCase(evaluationResult)) {
            // Trigger an action; for instance, send an email alert.
            return ResponseEntity.ok("Action executed: Email alert sent. Decision: " + evaluationResult);
        } else {
            return ResponseEntity.ok("Action executed: Hold (no action taken). Decision: " + evaluationResult);
        }
    }
}
