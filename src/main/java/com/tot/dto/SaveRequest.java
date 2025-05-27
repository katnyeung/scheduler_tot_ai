package com.tot.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Request to save a Tree of Thought")
public class SaveRequest {
    
    @Schema(description = "Tree structure in JSON format", required = true)
    private String treeJson;
    
    @Schema(description = "User-provided name for the tree", example = "Career Decision Tree")
    private String name;
    
    @Schema(description = "Description of the tree's purpose", example = "Helps decide between different career options")
    private String description;
    
    @Schema(description = "Tags for categorizing the tree", example = "[\"career\", \"decision\", \"personal\"]")
    private String[] tags;
    
    @Schema(description = "Whether to activate this tree immediately", defaultValue = "false")
    private Boolean activate = false;
    
    @Schema(description = "Session ID from the generation process")
    private String sessionId;

    public SaveRequest() {}

    public SaveRequest(String treeJson) {
        this.treeJson = treeJson;
    }

    public String getTreeJson() {
        return treeJson;
    }

    public void setTreeJson(String treeJson) {
        this.treeJson = treeJson;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String[] getTags() {
        return tags;
    }

    public void setTags(String[] tags) {
        this.tags = tags;
    }

    public Boolean getActivate() {
        return activate;
    }

    public void setActivate(Boolean activate) {
        this.activate = activate;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }
}