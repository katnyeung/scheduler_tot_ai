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
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.stream.Collectors;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

@RestController
@RequestMapping("/api/user")
@Tag(name = "Tree of Thought", description = "Manage the Tree of Thought (ToT)")
public class UserController {
    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    private final TotService totService;
    private final LLMService llmService;
    private final ObjectMapper objectMapper; // Added ObjectMapper

    @Autowired
    public UserController(TotService totService, LLMService llmService) {
        this.totService = totService;
        this.llmService = llmService;
        this.objectMapper = new ObjectMapper(); // Initialize ObjectMapper
    }

    @PostMapping("/generate")
    @Operation(summary = "Generate ToT", description = "Generate a new Tree of Thought structure using LLM")
    public ResponseEntity<String> generateTot(@RequestBody String prompt) {
        logger.info("Received request to generate ToT with prompt: {}", prompt);

        try {
            // Call the LLMService to generate a Tree of Thought
            String generatedTotJson = llmService.generateTreeOfThought(prompt);

            // Save the generated tree JSON using TotService and get the treeId
            String treeId = totService.saveTreeOfThought(generatedTotJson);

            // Return the treeId and info about saved tree
            String response = String.format("Generated and saved ToT with treeId: %s", treeId);
            return ResponseEntity.ok(response);
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

            // Save the refined tree JSON using TotService and get the treeId
            String treeId = totService.saveTreeOfThought(refinedTotJson);

            // Return the treeId and info about saved tree
            String response = String.format("Successfully refined and saved ToT with treeId: %s", treeId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error refining and saving ToT: {}", e.getMessage(), e); // Updated log message
            return ResponseEntity.internalServerError().body("Error refining and saving ToT: " + e.getMessage()); // Updated error response
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

            // Deserialize JSON to List of Maps
            List<Map<String, Object>> nodes;
            try {
                nodes = objectMapper.readValue(treeJson, new TypeReference<List<Map<String, Object>>>() {});
            } catch (Exception e) {
                logger.error("Error deserializing tree JSON: {}", e.getMessage(), e);
                return ResponseEntity.internalServerError().body("Error deserializing tree JSON: " + e.getMessage());
            }

            // Build the preview string
            String previewString = buildPreviewString(nodes, treeId);

            // Create a response with the preview string and validation result
            return ResponseEntity.ok()
                    .header("X-ToT-Validation", validationResult)
                    .body(previewString);
        } catch (Exception e) {
            logger.error("Error previewing ToT: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body("Error previewing ToT: " + e.getMessage());
        }
    }

    private String buildPreviewString(List<Map<String, Object>> nodes, String treeId) {
        StringBuilder sb = new StringBuilder();
        Set<String> childNodeIds = new HashSet<>();

        // Filter nodes by the current treeId and collect all child IDs
        List<Map<String, Object>> currentTreeNodes = nodes.stream()
                .filter(node -> treeId.equals(node.get("treeId")))
                .collect(Collectors.toList());

        for (Map<String, Object> node : currentTreeNodes) {
            @SuppressWarnings("unchecked")
            Map<String, String> children = (Map<String, String>) node.get("children");
            if (children != null) {
                childNodeIds.addAll(children.values());
            }
        }

        // Find root nodes (nodes in the current tree that are not children of any other node in this tree)
        List<Map<String, Object>> rootNodes = currentTreeNodes.stream()
                .filter(node -> !childNodeIds.contains(node.get("nodeId")))
                .collect(Collectors.toList());

        if (rootNodes.isEmpty() && !currentTreeNodes.isEmpty()) {
            // Fallback: if no explicit root found (e.g. circular dependencies or all nodes are children),
            // pick the first node as a pseudo-root to allow some output.
            // This might indicate a malformed tree for the given treeId.
             logger.warn("No root nodes found for treeId {}. Attempting to display from the first node.", treeId);
             if (!currentTreeNodes.isEmpty()) {
                 rootNodes.add(currentTreeNodes.get(0));
             }
        }


        for (Map<String, Object> rootNode : rootNodes) {
            buildNodeStringRecursive(rootNode, currentTreeNodes, sb, 0);
        }

        return sb.toString();
    }

    private void buildNodeStringRecursive(Map<String, Object> currentNode, List<Map<String, Object>> allNodesForTree, StringBuilder sb, int depth) {
        // Indentation
        for (int i = 0; i < depth; i++) {
            sb.append("  "); // 2 spaces per depth level
        }

        String nodeId = (String) currentNode.get("nodeId");
        String content = (String) currentNode.get("content");
        sb.append(nodeId).append(": ").append(content).append("\n");

        @SuppressWarnings("unchecked")
        Map<String, String> children = (Map<String, String>) currentNode.get("children");
        if (children != null && !children.isEmpty()) {
            for (Map.Entry<String, String> entry : children.entrySet()) {
                String childKey = entry.getKey();
                String childNodeId = entry.getValue();

                // Indentation for child branch type
                for (int i = 0; i < depth + 1; i++) {
                    sb.append("  ");
                }
                sb.append(childKey).append(" ->\n"); // Indicate the branch

                Map<String, Object> childNode = allNodesForTree.stream()
                        .filter(node -> childNodeId.equals(node.get("nodeId")))
                        .findFirst()
                        .orElse(null);

                if (childNode != null) {
                    buildNodeStringRecursive(childNode, allNodesForTree, sb, depth + 2); // Increment depth further for the actual child node
                } else {
                    // Indentation for missing child
                    for (int i = 0; i < depth + 2; i++) {
                        sb.append("  ");
                    }
                    sb.append(childNodeId).append(": [Node not found in provided list for this treeId]\n");
                }
            }
        }
    }

    @PostMapping("/save")
    @Operation(summary = "Save ToT", description = "Save the Tree of Thought structure into the database")
    public ResponseEntity<String> saveTot(@RequestBody String treeJson) {
        logger.info("Received request to save ToT from JSON");

        try {
            // Save the generated tree JSON using TotService and get the treeId
            String treeId = totService.saveTreeOfThought(treeJson);

            // Return the treeId and info about saved tree
            String response = String.format("Generated and saved ToT with treeId: %s", treeId);
            return ResponseEntity.ok(response);
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