package com.tot.service;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.tot.entity.TotNode;
import com.tot.repository.TotNodeRepository;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class TotService {

    private final ChatClient chatClient;
    private final ObjectMapper objectMapper;
    private final TotNodeRepository totNodeRepository;

    @Autowired
    public TotService(ChatClient.Builder chatClientBuilder, TotNodeRepository totNodeRepository) {
        this.chatClient = chatClientBuilder.build();
        this.objectMapper = new ObjectMapper();
        this.totNodeRepository = totNodeRepository;
    }

    /**
     * Update a Tree of Thought using an LLM
     * @param totData The current Tree of Thought data as JSON
     * @return Updated Tree of Thought data
     */
    public String updateTotWithLLM(String totData) {
        try {
            // Parse the input data
            JsonNode rootNode = objectMapper.readTree(totData);

            // Create a system prompt for the LLM
            String systemPrompt = """
                You are an expert at refining Tree of Thought (ToT) decision trees.
                
                Analyze the provided tree and suggest improvements:
                1. Identify logical gaps or missing decision points
                2. Enhance node criteria for clearer evaluation
                3. Suggest new branches where appropriate
                4. Keep the tree structure intact but improve its reasoning
                
                Respond ONLY with a valid JSON representing the improved tree.
                Maintain the same nodeId and treeId structure.
                """;

            // Create the user prompt with the tree data
            String userInput = "Here is the current Tree of Thought to refine: " + totData;

            // Query the LLM
            String llmResponse = this.chatClient.prompt()
                    .system(systemPrompt)
                    .user(userInput)
                    .call()
                    .content();

            // Validate the response is valid JSON
            objectMapper.readTree(llmResponse);

            // Return a structured response with status and data
            ObjectNode responseNode = objectMapper.createObjectNode();
            responseNode.put("status", "updated");
            responseNode.set("data", objectMapper.readTree(llmResponse));

            return objectMapper.writeValueAsString(responseNode);

        } catch (JsonProcessingException e) {
            // Handle JSON parsing errors
            try {
                ObjectNode errorNode = objectMapper.createObjectNode();
                errorNode.put("status", "error");
                errorNode.put("message", "Failed to process Tree of Thought data: " + e.getMessage());
                return objectMapper.writeValueAsString(errorNode);
            } catch (JsonProcessingException ex) {
                return "{ \"status\": \"error\", \"message\": \"Failed to process response\" }";
            }
        }
    }

    /**
     * Preview a Tree of Thought with validation and formatting
     * @param totData The Tree of Thought data as JSON
     * @return Formatted preview with validation status
     */
    public String previewTot(String totData) {
        try {
            // Parse and validate the JSON
            JsonNode rootNode = objectMapper.readTree(totData);

            // Extract and validate required fields
            boolean isValid = validateTotStructure(rootNode);

            // Generate statistics about the tree
            Map<String, Object> stats = generateTotStats(rootNode);

            // Create a formatted response
            ObjectNode responseNode = objectMapper.createObjectNode();
            responseNode.put("status", isValid ? "valid" : "invalid");

            // Add validation messages if invalid
            if (!isValid) {
                responseNode.put("message", "Tree structure is incomplete or invalid");
            }

            // Add statistics
            ObjectNode statsNode = responseNode.putObject("statistics");
            stats.forEach((key, value) -> {
                if (value instanceof Integer) {
                    statsNode.put(key, (Integer) value);
                } else if (value instanceof String) {
                    statsNode.put(key, (String) value);
                } else if (value instanceof Double) {
                    statsNode.put(key, (Double) value);
                } else if (value instanceof Boolean) {
                    statsNode.put(key, (Boolean) value);
                }
            });

            // Add the preview data
            responseNode.set("previewData", rootNode);

            return objectMapper.writeValueAsString(responseNode);

        } catch (JsonProcessingException e) {
            try {
                ObjectNode errorNode = objectMapper.createObjectNode();
                errorNode.put("status", "invalid");
                errorNode.put("message", "Invalid JSON format: " + e.getMessage());
                return objectMapper.writeValueAsString(errorNode);
            } catch (JsonProcessingException ex) {
                return "{ \"status\": \"invalid\", \"message\": \"Failed to process JSON\" }";
            }
        }
    }

    /**
     * Query the LLM with a custom prompt
     * @param prompt The user prompt
     * @return LLM response
     */
    public String queryLLM(String prompt) {
        try {
            // Create a basic system prompt for general ToT operations
            String systemPrompt = """
                You are an AI assistant specialized in Tree of Thought decision-making.
                Provide clear, concise, and logical responses to help improve decision trees.
                When providing code or data structures, use valid JSON format.
                """;

            // Query the LLM
            String llmResponse = this.chatClient.prompt()
                    .system(systemPrompt)
                    .user(prompt)
                    .call()
                    .content();

            // Return a structured response
            ObjectNode responseNode = objectMapper.createObjectNode();
            responseNode.put("status", "success");
            responseNode.put("response", llmResponse);

            return objectMapper.writeValueAsString(responseNode);

        } catch (Exception e) {
            try {
                ObjectNode errorNode = objectMapper.createObjectNode();
                errorNode.put("status", "error");
                errorNode.put("message", "Failed to query LLM: " + e.getMessage());
                return objectMapper.writeValueAsString(errorNode);
            } catch (JsonProcessingException ex) {
                return "{ \"status\": \"error\", \"message\": \"Failed to process response\" }";
            }
        }
    }

    /**
     * Save a Tree of Thought to the database
     * @param totData The Tree of Thought data as JSON
     * @return Status of the save operation
     */
    @Transactional
    public String saveTot(String totData) {
        try {
            // Parse the input data
            JsonNode rootNode = objectMapper.readTree(totData);

            // Check if this is a single node or an array of nodes
            if (rootNode.isArray()) {
                // Process multiple nodes
                ArrayNode nodesArray = (ArrayNode) rootNode;
                for (JsonNode nodeJson : nodesArray) {
                    saveNodeFromJson(nodeJson);
                }
            } else {
                // Process a single node
                saveNodeFromJson(rootNode);
            }

            // Return success response
            ObjectNode responseNode = objectMapper.createObjectNode();
            responseNode.put("status", "saved");
            responseNode.put("message", "Tree of Thought saved successfully");

            return objectMapper.writeValueAsString(responseNode);

        } catch (Exception e) {
            try {
                ObjectNode errorNode = objectMapper.createObjectNode();
                errorNode.put("status", "error");
                errorNode.put("message", "Failed to save Tree of Thought: " + e.getMessage());
                return objectMapper.writeValueAsString(errorNode);
            } catch (JsonProcessingException ex) {
                return "{ \"status\": \"error\", \"message\": \"Failed to process response\" }";
            }
        }
    }

    /**
     * Export a complete Tree of Thought by treeId
     * @param treeId The ID of the tree to export
     * @return JSON representation of the entire tree
     */
    public String exportTree(String treeId) {
        try {
            // Get all nodes for the tree
            List<TotNode> nodes = totNodeRepository.findByTreeId(treeId);

            if (nodes.isEmpty()) {
                ObjectNode errorNode = objectMapper.createObjectNode();
                errorNode.put("status", "error");
                errorNode.put("message", "Tree not found with ID: " + treeId);
                return objectMapper.writeValueAsString(errorNode);
            }

            // Create an array to hold all nodes
            ArrayNode treeArray = objectMapper.createArrayNode();

            // Add each node to the array
            for (TotNode node : nodes) {
                ObjectNode nodeObj = objectMapper.createObjectNode();
                nodeObj.put("nodeId", node.getNodeId());
                nodeObj.put("treeId", node.getTreeId());
                nodeObj.put("content", node.getContent());
                nodeObj.put("criteria", node.getCriteria());

                // Add children map
                ObjectNode childrenObj = nodeObj.putObject("children");
                if (node.getChildren() != null) {
                    for (Map.Entry<String, String> entry : node.getChildren().entrySet()) {
                        childrenObj.put(entry.getKey(), entry.getValue());
                    }
                }

                treeArray.add(nodeObj);
            }

            // Create the response
            ObjectNode responseNode = objectMapper.createObjectNode();
            responseNode.put("status", "success");
            responseNode.put("treeId", treeId);
            responseNode.put("nodeCount", nodes.size());
            responseNode.set("tree", treeArray);

            return objectMapper.writeValueAsString(responseNode);

        } catch (Exception e) {
            try {
                ObjectNode errorNode = objectMapper.createObjectNode();
                errorNode.put("status", "error");
                errorNode.put("message", "Failed to export tree: " + e.getMessage());
                return objectMapper.writeValueAsString(errorNode);
            } catch (JsonProcessingException ex) {
                return "{ \"status\": \"error\", \"message\": \"Failed to process response\" }";
            }
        }
    }

    // Helper methods

    /**
     * Save a node from JSON to the database
     */
    private void saveNodeFromJson(JsonNode nodeJson) throws JsonProcessingException {
        String nodeId = nodeJson.path("nodeId").asText();
        String treeId = nodeJson.path("treeId").asText();

        // Check if the node already exists
        Optional<TotNode> existingNode = totNodeRepository.findByNodeIdAndTreeId(nodeId, treeId);

        TotNode node;
        if (existingNode.isPresent()) {
            node = existingNode.get();
        } else {
            node = new TotNode();
            node.setNodeId(nodeId);
            node.setTreeId(treeId);
        }

        // Update node properties
        node.setContent(nodeJson.path("content").asText());
        node.setCriteria(nodeJson.path("criteria").asText());

        // Process children
        Map<String, String> children = new HashMap<>();
        JsonNode childrenNode = nodeJson.path("children");
        if (childrenNode.isObject()) {
            childrenNode.fields().forEachRemaining(entry ->
                    children.put(entry.getKey(), entry.getValue().asText())
            );
        }
        node.setChildren(children);

        // Save the node
        totNodeRepository.save(node);
    }

    /**
     * Validate the structure of a Tree of Thought
     */
    private boolean validateTotStructure(JsonNode rootNode) {
        // If it's an array, validate each node
        if (rootNode.isArray()) {
            for (JsonNode node : rootNode) {
                if (!validateNodeStructure(node)) {
                    return false;
                }
            }
            return true;
        } else {
            // Single node validation
            return validateNodeStructure(rootNode);
        }
    }

    /**
     * Validate an individual node structure
     */
    private boolean validateNodeStructure(JsonNode node) {
        // Check required fields
        return node.has("nodeId") && !node.path("nodeId").asText().isEmpty() &&
                node.has("treeId") && !node.path("treeId").asText().isEmpty() &&
                node.has("content") && !node.path("content").asText().isEmpty() &&
                node.has("criteria") && !node.path("criteria").asText().isEmpty();
    }

    /**
     * Generate statistics about a Tree of Thought
     */
    private Map<String, Object> generateTotStats(JsonNode rootNode) {
        Map<String, Object> stats = new HashMap<>();

        // Count total nodes
        int nodeCount = rootNode.isArray() ? rootNode.size() : 1;
        stats.put("nodeCount", nodeCount);

        // If it's a single node, we can't calculate much more
        if (!rootNode.isArray()) {
            stats.put("treeId", rootNode.path("treeId").asText());
            return stats;
        }

        // For arrays, we can do more analysis
        Set<String> treeIds = new HashSet<>();
        Set<String> nodeIds = new HashSet<>();
        Map<String, Integer> childrenCounts = new HashMap<>();

        // Collect all nodeIds and count references
        for (JsonNode node : rootNode) {
            String nodeId = node.path("nodeId").asText();
            String treeId = node.path("treeId").asText();

            nodeIds.add(nodeId);
            treeIds.add(treeId);

            // Count children references
            JsonNode childrenNode = node.path("children");
            if (childrenNode.isObject()) {
                childrenNode.fields().forEachRemaining(entry -> {
                    String childId = entry.getValue().asText();
                    childrenCounts.put(childId, childrenCounts.getOrDefault(childId, 0) + 1);
                });
            }
        }

        // Find root nodes (not referenced as children)
        Set<String> rootNodes = new HashSet<>(nodeIds);
        rootNodes.removeAll(childrenCounts.keySet());

        // Find leaf nodes (have no children)
        Set<String> leafNodes = new HashSet<>();
        for (JsonNode node : rootNode) {
            String nodeId = node.path("nodeId").asText();
            JsonNode childrenNode = node.path("children");

            if (!childrenNode.isObject() || childrenNode.size() == 0) {
                leafNodes.add(nodeId);
            }
        }

        // Add statistics
        stats.put("treeCount", treeIds.size());
        stats.put("rootNodeCount", rootNodes.size());
        stats.put("leafNodeCount", leafNodes.size());
        stats.put("maxDepth", estimateMaxDepth(rootNode, rootNodes));

        return stats;
    }

    /**
     * Estimate the maximum depth of the tree
     */
    private int estimateMaxDepth(JsonNode rootNode, Set<String> rootNodeIds) {
        // Build a map of nodeId to node
        Map<String, JsonNode> nodeMap = new HashMap<>();
        for (JsonNode node : rootNode) {
            nodeMap.put(node.path("nodeId").asText(), node);
        }

        // Find max depth from each root
        int maxDepth = 0;
        for (String rootId : rootNodeIds) {
            int depth = calculateDepth(rootId, nodeMap, new HashSet<>());
            maxDepth = Math.max(maxDepth, depth);
        }

        return maxDepth;
    }

    /**
     * Calculate depth of a node recursively
     */
    private int calculateDepth(String nodeId, Map<String, JsonNode> nodeMap, Set<String> visited) {
        // Prevent cycles
        if (visited.contains(nodeId)) {
            return 0;
        }

        visited.add(nodeId);

        // Get the node
        JsonNode node = nodeMap.get(nodeId);
        if (node == null) {
            return 1; // Referenced but not found
        }

        // No children
        JsonNode childrenNode = node.path("children");
        if (!childrenNode.isObject() || childrenNode.size() == 0) {
            return 1;
        }

        // Find max child depth
        AtomicInteger maxDepth = new AtomicInteger();
        final int maxChildDepth = 0;
        final Set<String> currentVisited = new HashSet<>(visited);

        childrenNode.fields().forEachRemaining(entry -> {
            String childId = entry.getValue().asText();
            int childDepth = calculateDepth(childId, nodeMap, currentVisited);
            maxDepth.set(Math.max(maxChildDepth, childDepth));
        });

        return 1 + maxDepth.get();
    }
}