package com.tot.controller;

import com.tot.entity.TotNode;
import com.tot.service.LLMService;
import com.tot.service.TotService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/user")
@Tag(name = "Tree of Thought", description = "Manage the Tree of Thought (ToT)")
public class UserController {
    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    private final TotService totService;
    private final LLMService llmService;

    @Autowired
    public UserController(TotService totService, LLMService llmService) {
        this.totService = totService;
        this.llmService = llmService;
    }

    @PostMapping("/generate")
    @Operation(summary = "Generate ToT", description = "Generate a new Tree of Thought structure using LLM")
    public ResponseEntity<String> generateTot(@RequestBody String prompt) {
        logger.info("Received request to generate ToT with prompt: {}", prompt);

        try {
            // Call the LLMService to generate a Tree of Thought
            String generatedTotJson = llmService.generateTreeOfThought(prompt);

            // Return the generated tree JSON
            return ResponseEntity.ok(generatedTotJson);
        } catch (Exception e) {
            logger.error("Error generating ToT: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body("Error generating ToT: " + e.getMessage());
        }
    }

    @PostMapping("/refine")
    @Operation(summary = "Refine ToT", description = "Refine an existing Tree of Thought with latest data and details")
    public ResponseEntity<String> refineTot(@RequestBody String treeJson) {
        logger.info("Received request to refine ToT");

        try {
            // Call the LLMService to refine the existing Tree of Thought
            String refinedTotJson = llmService.refineTreeOfThought(treeJson);

            // Return the refined tree JSON
            return ResponseEntity.ok(refinedTotJson);
        } catch (Exception e) {
            logger.error("Error refining ToT: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body("Error refining ToT: " + e.getMessage());
        }
    }

    @PostMapping("/preview")
    @Operation(summary = "Preview ToT", description = "Preview the Tree of Thought structure and validate format")
    public ResponseEntity<String> previewTot(@RequestParam String treeId) {
        logger.info("Received request to preview ToT with ID: {}", treeId);

        try {
            // Use the TotService to fetch and generate a JSON preview
            String treeJson = totService.getTreeOfThought(treeId);

            // Validate the tree structure
            String validationResult = llmService.validateTree(treeJson);

            // Create a response with both the tree JSON and validation result
            return ResponseEntity.ok()
                    .header("X-ToT-Validation", validationResult)
                    .body(treeJson);
        } catch (Exception e) {
            logger.error("Error previewing ToT: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body("Error previewing ToT: " + e.getMessage());
        }
    }

    @PostMapping("/save")
    @Operation(summary = "Save ToT", description = "Save the Tree of Thought structure into the database")
    public ResponseEntity<String> saveTot(@RequestBody String treeJson) {
        logger.info("Received request to save ToT from JSON");

        try {
            // Use TotService to save the tree from JSON
            List<TotNode> savedNodes = totService.saveTreeOfThought(treeJson);

            // Return success response
            return ResponseEntity.ok(String.format("Saved %d nodes successfully", savedNodes.size()));
        } catch (Exception e) {
            logger.error("Error saving ToT: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body("Error saving ToT: " + e.getMessage());
        }
    }

    @GetMapping("/tree/{treeId}")
    @Operation(summary = "Get ToT", description = "Get a complete Tree of Thought by its ID")
    public ResponseEntity<String> getTree(@PathVariable String treeId) {
        logger.info("Received request to get ToT with ID: {}", treeId);

        try {
            // Get the tree JSON from the service
            String treeJson = totService.getTreeOfThought(treeId);

            // Return the tree JSON
            return ResponseEntity.ok(treeJson);
        } catch (Exception e) {
            logger.error("Error getting ToT: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body("Error getting ToT: " + e.getMessage());
        }
    }

    @PostMapping("/validate")
    @Operation(summary = "Validate ToT", description = "Validate a Tree of Thought structure")
    public ResponseEntity<String> validateTot(@RequestBody String treeJson) {
        logger.info("Received request to validate ToT");

        try {
            // Validate the tree structure
            String validationResult = llmService.validateTree(treeJson);

            // Return the validation result
            return ResponseEntity.ok(validationResult);
        } catch (Exception e) {
            logger.error("Error validating ToT: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body("Error validating ToT: " + e.getMessage());
        }
    }
}