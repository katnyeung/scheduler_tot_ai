package com.tot.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "Response from saving a Tree of Thought")
public class SaveResponse {
    
    @Schema(description = "Unique ID of the saved tree")
    private String treeId;
    
    @Schema(description = "Number of nodes saved")
    private Integer nodeCount;
    
    @Schema(description = "Status of the save operation")
    private String status;
    
    @Schema(description = "Timestamp when the tree was saved")
    private LocalDateTime savedAt;
    
    @Schema(description = "Whether the tree was activated")
    private Boolean activated;
    
    @Schema(description = "Message describing the save result")
    private String message;

    public SaveResponse() {}

    public SaveResponse(String treeId, Integer nodeCount, String status) {
        this.treeId = treeId;
        this.nodeCount = nodeCount;
        this.status = status;
        this.savedAt = LocalDateTime.now();
    }

    public String getTreeId() {
        return treeId;
    }

    public void setTreeId(String treeId) {
        this.treeId = treeId;
    }

    public Integer getNodeCount() {
        return nodeCount;
    }

    public void setNodeCount(Integer nodeCount) {
        this.nodeCount = nodeCount;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getSavedAt() {
        return savedAt;
    }

    public void setSavedAt(LocalDateTime savedAt) {
        this.savedAt = savedAt;
    }

    public Boolean getActivated() {
        return activated;
    }

    public void setActivated(Boolean activated) {
        this.activated = activated;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}