package com.tot.controller;

import com.tot.service.PerplexityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/action")
@Tag(name = "Action", description = "Trigger actions based on TOT decisions")
public class ActionController {

    private final PerplexityService perplexityService;

    @Autowired
    public ActionController(PerplexityService perplexityService) {
        this.perplexityService = perplexityService;
    }

    @PostMapping("/execute")
    @Operation(summary = "Execute action", description = "Perform an action based on the evaluated TOT (e.g., send alert email)")
    public ResponseEntity<String> executeAction(@RequestParam String totData) {
        // Construct a prompt string
        String prompt = "please walk through the attached tree, and give me a true or false response " + totData;
        // Call the generateCompletion method of the injected PerplexityService instance
        String result = perplexityService.generateCompletion(prompt);
        // Check if the result string starts with "Error:"
        if (result != null && result.startsWith("Error:")) {
            // If it does, return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result)
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
        } else {
            // Otherwise, return ResponseEntity.ok(result)
            return ResponseEntity.ok(result);
        }
    }
}
