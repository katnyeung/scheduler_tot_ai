package com.tot.controller;

import com.tot.dto.*;
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

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/api/user")
@Tag(name = "Tree of Thought UI", description = "UI-friendly Tree of Thought management endpoints")
@CrossOrigin(origins = "*") // Enable CORS for UI integration
public class UserController {
    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    private final TotService totService;
    private final LLMService llmService;
    
    // In-memory session storage for UI workflow (consider Redis for production)
    private final Map<String, String> sessionStorage = new ConcurrentHashMap<>();

    @Autowired
    public UserController(TotService totService, LLMService llmService) {
        this.totService = totService;
        this.llmService = llmService;
    }

    @PostMapping("/generate")
    @Operation(summary = "Generate ToT", description = "Generate a new Tree of Thought structure from user prompt")
    public ResponseEntity<GenerateResponse> generateTot(@RequestBody GenerateRequest request) {
        logger.info("Received request to generate ToT with prompt: {}", request.getPrompt());

        try {
            // Generate a unique session ID for this generation
            String sessionId = UUID.randomUUID().toString();
            
            // Call the LLMService to generate a Tree of Thought
            String generatedTotJson = llmService.generateTreeOfThought(request.getPrompt());
            
            // Store the generated tree in session for later use
            sessionStorage.put(sessionId, generatedTotJson);
            
            // Create response with metadata
            GenerateResponse response = new GenerateResponse(sessionId, generatedTotJson, 
                "Tree of Thought generated successfully for: " + request.getPrompt());
            
            // Add metadata (basic analysis of the generated tree)
            response.setNodeCount(countNodes(generatedTotJson));
            response.setDepth(calculateDepth(generatedTotJson));
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error generating ToT: {}", e.getMessage(), e);
            GenerateResponse errorResponse = new GenerateResponse();
            errorResponse.setStatus("error");
            errorResponse.setSummary("Error generating ToT: " + e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
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

    @GetMapping("/preview")
    @Operation(summary = "Preview ToT", description = "Get UI-friendly preview of Tree of Thought structure")
    public ResponseEntity<PreviewResponse> previewTot(@RequestParam String sessionId) {
        logger.info("Received request to preview ToT with session ID: {}", sessionId);

        try {
            // Get tree from session or database
            String treeJson = sessionStorage.getOrDefault(sessionId, null);
            if (treeJson == null) {
                // Try to get from database if not in session
                treeJson = totService.getTreeOfThought(sessionId);
            }

            if (treeJson == null) {
                return ResponseEntity.notFound().build();
            }

            // Create UI-friendly preview response
            PreviewResponse response = new PreviewResponse();
            
            // Set raw JSON
            response.setRawJson(treeJson);
            
            // Create tree structure for UI
            response.setTreeStructure(createTreeStructure(treeJson));
            
            // Validate the tree
            response.setValidation(validateTreeForUI(treeJson));
            
            // Generate sample walkthrough paths
            response.setSamplePaths(generateSamplePaths(treeJson));

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error previewing ToT: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/save")
    @Operation(summary = "Save ToT", description = "Save the Tree of Thought structure with metadata")
    public ResponseEntity<SaveResponse> saveTot(@RequestBody SaveRequest request) {
        logger.info("Received request to save ToT: {}", request.getName());

        try {
            // Get tree JSON from request or session
            String treeJson = request.getTreeJson();
            if (treeJson == null && request.getSessionId() != null) {
                treeJson = sessionStorage.get(request.getSessionId());
            }
            
            if (treeJson == null) {
                SaveResponse errorResponse = new SaveResponse();
                errorResponse.setStatus("error");
                errorResponse.setMessage("No tree data found");
                return ResponseEntity.badRequest().body(errorResponse);
            }

            // Save the tree using TotService
            List<TotNode> savedNodes = totService.saveTreeOfThought(treeJson);
            
            // Generate a tree ID (could be the root node ID or a generated UUID)
            String treeId = savedNodes.isEmpty() ? UUID.randomUUID().toString() : savedNodes.get(0).getId();

            // Create response
            SaveResponse response = new SaveResponse(treeId, savedNodes.size(), "success");
            response.setActivated(request.getActivate());
            response.setMessage(String.format("Successfully saved tree '%s' with %d nodes", 
                request.getName() != null ? request.getName() : "Unnamed Tree", savedNodes.size()));

            // Clean up session if provided
            if (request.getSessionId() != null) {
                sessionStorage.remove(request.getSessionId());
            }

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error saving ToT: {}", e.getMessage(), e);
            SaveResponse errorResponse = new SaveResponse();
            errorResponse.setStatus("error");
            errorResponse.setMessage("Error saving ToT: " + e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
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
    public ResponseEntity<PreviewResponse.ValidationResult> validateTot(@RequestBody String treeJson) {
        logger.info("Received request to validate ToT");

        try {
            // Validate the tree structure using UI-friendly format
            PreviewResponse.ValidationResult validation = validateTreeForUI(treeJson);
            return ResponseEntity.ok(validation);
        } catch (Exception e) {
            logger.error("Error validating ToT: {}", e.getMessage(), e);
            PreviewResponse.ValidationResult errorResult = new PreviewResponse.ValidationResult();
            errorResult.setIsValid(false);
            errorResult.setMessage("Error validating ToT: " + e.getMessage());
            errorResult.setScore(0);
            errorResult.setIssues(Arrays.asList("Validation failed due to system error"));
            return ResponseEntity.internalServerError().body(errorResult);
        }
    }

    @PostMapping("/walkthrough")
    @Operation(summary = "Simulate Tree Walkthrough", description = "Simulate walking through the tree with provided input data")
    public ResponseEntity<Map<String, Object>> simulateWalkthrough(
            @RequestParam String sessionId,
            @RequestBody Map<String, Object> inputData) {
        logger.info("Received request to simulate walkthrough for session: {}", sessionId);

        try {
            String treeJson = sessionStorage.get(sessionId);
            if (treeJson == null) {
                treeJson = totService.getTreeOfThought(sessionId);
            }

            if (treeJson == null) {
                return ResponseEntity.notFound().build();
            }

            // Simulate the walkthrough (this would need implementation based on your tree structure)
            Map<String, Object> result = new HashMap<>();
            result.put("sessionId", sessionId);
            result.put("path", simulateTreePath(treeJson, inputData));
            result.put("finalDecision", "Sample decision based on input");
            result.put("confidence", 0.85);
            result.put("reasoning", "This is a simulated walkthrough for UI preview");

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("Error simulating walkthrough: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    // Helper methods for UI support
    private Integer countNodes(String treeJson) {
        // Simple implementation - count occurrences of "id" field
        return (int) treeJson.chars().filter(ch -> ch == '{').count();
    }

    private Integer calculateDepth(String treeJson) {
        // Simple implementation - could be enhanced based on actual tree structure
        return 3; // Default depth for now
    }

    private PreviewResponse.TreeStructure createTreeStructure(String treeJson) {
        PreviewResponse.TreeStructure structure = new PreviewResponse.TreeStructure();
        
        // Create a sample root node (this should parse the actual JSON)
        PreviewResponse.TreeNode root = new PreviewResponse.TreeNode();
        root.setId("root");
        root.setContent("Root Decision Node");
        root.setType("decision");
        root.setCriteria("Initial decision criteria");
        root.setChildren(new ArrayList<>());
        
        structure.setRoot(root);
        structure.setTotalNodes(countNodes(treeJson));
        structure.setMaxDepth(calculateDepth(treeJson));
        
        return structure;
    }

    private PreviewResponse.ValidationResult validateTreeForUI(String treeJson) {
        try {
            // Use existing LLM validation but convert to UI format
            String llmResult = llmService.validateTree(treeJson);
            
            PreviewResponse.ValidationResult result = new PreviewResponse.ValidationResult();
            result.setIsValid(llmResult.toLowerCase().contains("true") || llmResult.toLowerCase().contains("valid"));
            result.setMessage(llmResult);
            result.setScore(result.getIsValid() ? 85 : 35);
            result.setIssues(result.getIsValid() ? new ArrayList<>() : 
                Arrays.asList("Tree structure needs improvement"));
            
            return result;
        } catch (Exception e) {
            PreviewResponse.ValidationResult result = new PreviewResponse.ValidationResult();
            result.setIsValid(false);
            result.setMessage("Validation failed: " + e.getMessage());
            result.setScore(0);
            result.setIssues(Arrays.asList("System error during validation"));
            return result;
        }
    }

    private List<PreviewResponse.WalkthroughPath> generateSamplePaths(String treeJson) {
        List<PreviewResponse.WalkthroughPath> paths = new ArrayList<>();
        
        // Generate sample paths for UI preview
        PreviewResponse.WalkthroughPath path1 = new PreviewResponse.WalkthroughPath();
        path1.setDescription("Optimistic scenario");
        path1.setNodeIds(Arrays.asList("root", "decision1", "action1"));
        path1.setOutcome("Positive outcome achieved");
        paths.add(path1);
        
        PreviewResponse.WalkthroughPath path2 = new PreviewResponse.WalkthroughPath();
        path2.setDescription("Conservative scenario");
        path2.setNodeIds(Arrays.asList("root", "decision2", "action2"));
        path2.setOutcome("Safe alternative chosen");
        paths.add(path2);
        
        return paths;
    }

    private List<String> simulateTreePath(String treeJson, Map<String, Object> inputData) {
        // Simulate following a path through the tree based on input
        return Arrays.asList("root", "decision_node_1", "action_node_final");
    }
}