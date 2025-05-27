package com.tot.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.Map;

@Schema(description = "Response for tree preview with UI-friendly structure")
public class PreviewResponse {
    
    @Schema(description = "Tree structure formatted for UI rendering")
    private TreeStructure treeStructure;
    
    @Schema(description = "Validation result of the tree")
    private ValidationResult validation;
    
    @Schema(description = "Sample walkthrough paths for demonstration")
    private List<WalkthroughPath> samplePaths;
    
    @Schema(description = "Raw tree JSON")
    private String rawJson;

    public static class TreeStructure {
        @Schema(description = "Root node of the tree")
        private TreeNode root;
        
        @Schema(description = "Total number of nodes")
        private Integer totalNodes;
        
        @Schema(description = "Maximum depth")
        private Integer maxDepth;

        public TreeNode getRoot() {
            return root;
        }

        public void setRoot(TreeNode root) {
            this.root = root;
        }

        public Integer getTotalNodes() {
            return totalNodes;
        }

        public void setTotalNodes(Integer totalNodes) {
            this.totalNodes = totalNodes;
        }

        public Integer getMaxDepth() {
            return maxDepth;
        }

        public void setMaxDepth(Integer maxDepth) {
            this.maxDepth = maxDepth;
        }
    }

    public static class TreeNode {
        @Schema(description = "Node ID")
        private String id;
        
        @Schema(description = "Node content/description")
        private String content;
        
        @Schema(description = "Node type (decision, action, etc.)")
        private String type;
        
        @Schema(description = "Child nodes")
        private List<TreeNode> children;
        
        @Schema(description = "Decision criteria")
        private String criteria;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public List<TreeNode> getChildren() {
            return children;
        }

        public void setChildren(List<TreeNode> children) {
            this.children = children;
        }

        public String getCriteria() {
            return criteria;
        }

        public void setCriteria(String criteria) {
            this.criteria = criteria;
        }
    }

    public static class ValidationResult {
        @Schema(description = "Whether the tree is valid")
        private Boolean isValid;
        
        @Schema(description = "Validation message")
        private String message;
        
        @Schema(description = "Validation score (0-100)")
        private Integer score;
        
        @Schema(description = "Issues found during validation")
        private List<String> issues;

        public Boolean getIsValid() {
            return isValid;
        }

        public void setIsValid(Boolean isValid) {
            this.isValid = isValid;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public Integer getScore() {
            return score;
        }

        public void setScore(Integer score) {
            this.score = score;
        }

        public List<String> getIssues() {
            return issues;
        }

        public void setIssues(List<String> issues) {
            this.issues = issues;
        }
    }

    public static class WalkthroughPath {
        @Schema(description = "Path description")
        private String description;
        
        @Schema(description = "Sequence of node IDs in the path")
        private List<String> nodeIds;
        
        @Schema(description = "Path outcome")
        private String outcome;

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public List<String> getNodeIds() {
            return nodeIds;
        }

        public void setNodeIds(List<String> nodeIds) {
            this.nodeIds = nodeIds;
        }

        public String getOutcome() {
            return outcome;
        }

        public void setOutcome(String outcome) {
            this.outcome = outcome;
        }
    }

    public TreeStructure getTreeStructure() {
        return treeStructure;
    }

    public void setTreeStructure(TreeStructure treeStructure) {
        this.treeStructure = treeStructure;
    }

    public ValidationResult getValidation() {
        return validation;
    }

    public void setValidation(ValidationResult validation) {
        this.validation = validation;
    }

    public List<WalkthroughPath> getSamplePaths() {
        return samplePaths;
    }

    public void setSamplePaths(List<WalkthroughPath> samplePaths) {
        this.samplePaths = samplePaths;
    }

    public String getRawJson() {
        return rawJson;
    }

    public void setRawJson(String rawJson) {
        this.rawJson = rawJson;
    }
}