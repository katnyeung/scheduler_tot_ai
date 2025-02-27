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
    public List<TotNode> saveTreeOfThought(String treeJson) {
        logger.info("Saving Tree of Thought from JSON");

        try {
            // Parse the JSON into a list of node objects
            List<Map<String, Object>> nodeList = objectMapper.readValue(
                    treeJson, new TypeReference<List<Map<String, Object>>>() {});

            List<TotNode> savedNodes = new ArrayList<>();

            // Convert and save each node
            for (Map<String, Object> nodeMap : nodeList) {
                TotNode node = new TotNode();

                // Set basic properties
                node.setNodeId((String) nodeMap.get("nodeId"));
                node.setTreeId((String) nodeMap.get("treeId"));
                node.setContent((String) nodeMap.get("content"));
                node.setCriteria((String) nodeMap.get("criteria"));

                // Handle children map
                @SuppressWarnings("unchecked")
                Map<String, String> children = (Map<String, String>) nodeMap.get("children");
                if (children == null) {
                    children = new HashMap<>();
                }
                node.setChildren(children);

                // Save node to repository
                savedNodes.add(totNodeRepository.save(node));
                logger.debug("Saved node: {}", node.getNodeId());
            }

            logger.info("Saved {} nodes for Tree of Thought", savedNodes.size());
            return savedNodes;

        } catch (Exception e) {
            logger.error("Error saving tree from JSON: {}", e.getMessage());
            throw new RuntimeException("Failed to save Tree of Thought from JSON", e);
        }
    }
}