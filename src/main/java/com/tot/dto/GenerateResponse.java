package com.tot.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Response from generating a Tree of Thought")
public class GenerateResponse {
    
    @Schema(description = "Unique session ID for this generated tree")
    private String sessionId;
    
    @Schema(description = "Generated tree structure in JSON format")
    private String treeJson;
    
    @Schema(description = "Human-readable summary of the tree")
    private String summary;
    
    @Schema(description = "Number of nodes in the generated tree")
    private Integer nodeCount;
    
    @Schema(description = "Maximum depth of the generated tree")
    private Integer depth;
    
    @Schema(description = "Status of the generation process")
    private String status;

    public GenerateResponse() {}

    public GenerateResponse(String sessionId, String treeJson, String summary) {
        this.sessionId = sessionId;
        this.treeJson = treeJson;
        this.summary = summary;
        this.status = "success";
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getTreeJson() {
        return treeJson;
    }

    public void setTreeJson(String treeJson) {
        this.treeJson = treeJson;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public Integer getNodeCount() {
        return nodeCount;
    }

    public void setNodeCount(Integer nodeCount) {
        this.nodeCount = nodeCount;
    }

    public Integer getDepth() {
        return depth;
    }

    public void setDepth(Integer depth) {
        this.depth = depth;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}