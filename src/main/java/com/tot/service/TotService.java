package com.tot.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.tot.entity.TotNode;
import com.tot.repository.TotNodeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service for managing Tree of Thought operations
 */
@Service
public class TotService {
    private static final Logger logger = LoggerFactory.getLogger(TotService.class);
    private final TotNodeRepository totNodeRepository;
    private final ObjectMapper objectMapper;

    @Autowired
    public TotService(TotNodeRepository totNodeRepository) {
        this.totNodeRepository = totNodeRepository;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Get a Tree of Thought by ID as JSON
     * @param treeId ID of the tree to retrieve
     * @return JSON string representation of the tree
     */
    public String getTreeOfThought(String treeId) {
        logger.info("Retrieving Tree of Thought: {}", treeId);

        try {
            // Get all nodes for the tree
            List<TotNode> nodes = totNodeRepository.findByTreeId(treeId);

            if (nodes.isEmpty()) {
                logger.warn("Tree not found: {}", treeId);
                throw new IllegalArgumentException("Tree not found: " + treeId);
            }

            // Convert nodes to JSON array
            ArrayNode treeArray = objectMapper.createArrayNode();

            for (TotNode node : nodes) {
                ObjectNode nodeJson = objectMapper.createObjectNode();
                nodeJson.put("nodeId", node.getNodeId());
                nodeJson.put("treeId", node.getTreeId());
                nodeJson.put("content", node.getContent());
                nodeJson.put("criteria", node.getCriteria());

                // Add children
                ObjectNode childrenObj = nodeJson.putObject("children");
                if (node.getChildren() != null) {
                    node.getChildren().forEach(childrenObj::put);
                }

                treeArray.add(nodeJson);
            }

            return objectMapper.writeValueAsString(treeArray);

        } catch (JsonProcessingException e) {
            logger.error("Error serializing tree: {}", e.getMessage());
            throw new RuntimeException("Failed to serialize tree", e);
        }
    }

    /**
     * Save a Tree of Thought from JSON representation
     * @param treeJson JSON string representation of the tree
     * @return List of saved nodes
     */
    @Transactional
    public String saveTreeOfThought(String treeJson) {
        logger.info("Saving Tree of Thought from JSON");

        try {
            // Parse the JSON into a list of node objects
            List<Map<String, Object>> nodeList = objectMapper.readValue(
                    treeJson, new TypeReference<>() {
                    });

            List<TotNode> savedNodes = new ArrayList<>();

            // Convert and save each node
            for (Map<String, Object> nodeMap : nodeList) {
                TotNode node = new TotNode();

                // Explicitly set properties using JSON key names to avoid field order dependency
                String nodeId = (String) nodeMap.get("nodeId");
                String treeId = (String) nodeMap.get("treeId");
                String content = (String) nodeMap.get("content");
                String criteria = (String) nodeMap.get("criteria");
                
                // Log the values being set for debugging
                logger.debug("Setting node - nodeId: {}, treeId: {}, content: {}, criteria: {}", 
                            nodeId, treeId, content, criteria);
                
                // Set properties explicitly
                node.setNodeId(nodeId);
                node.setTreeId(treeId);
                node.setContent(content);
                node.setCriteria(criteria);

                // Handle children map
                @SuppressWarnings("unchecked")
                Map<String, String> children = (Map<String, String>) nodeMap.get("children");
                if (children == null) {
                    children = new HashMap<>();
                }
                node.setChildren(children);

                // Save node to repository
                TotNode savedNode = totNodeRepository.save(node);
                savedNodes.add(savedNode);
                logger.debug("Saved node: nodeId={}, treeId={}, content={}", 
                            savedNode.getNodeId(), savedNode.getTreeId(), savedNode.getContent());
            }

            logger.info("Saved {} nodes for Tree of Thought", savedNodes.size());

            if (!savedNodes.isEmpty()) {
                String treeId = savedNodes.get(0).getTreeId();
                logger.info("Returning treeId: {}", treeId);
                return treeId;
            } else {
                logger.warn("No nodes were saved, returning null treeId");
                return null;
            }

        } catch (Exception e) {
            logger.error("Error saving tree from JSON: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to save Tree of Thought from JSON", e);
        }
    }

    /**
     * Get all available tree IDs
     * @return List of tree IDs
     */
    public List<String> getAllTreeIds() {
        logger.info("Retrieving all tree IDs");
        
        try {
            List<String> treeIds = totNodeRepository.findAllTreeIds();
            logger.info("Found {} tree IDs", treeIds.size());
            return treeIds;
        } catch (Exception e) {
            logger.error("Error retrieving tree IDs: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to retrieve tree IDs", e);
        }
    }
}