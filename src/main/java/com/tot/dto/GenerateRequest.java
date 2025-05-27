package com.tot.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Request to generate a Tree of Thought")
public class GenerateRequest {
    
    @Schema(description = "User prompt for generating the ToT", required = true, example = "Create a decision tree for choosing a career path")
    private String prompt;
    
    @Schema(description = "Optional context or additional information", example = "Consider factors like salary, work-life balance, and personal interests")
    private String context;
    
    @Schema(description = "Maximum depth for the tree", example = "5", defaultValue = "3")
    private Integer maxDepth = 3;
    
    @Schema(description = "Preferred number of branches per node", example = "3", defaultValue = "2")
    private Integer branchingFactor = 2;

    public GenerateRequest() {}

    public GenerateRequest(String prompt) {
        this.prompt = prompt;
    }

    public String getPrompt() {
        return prompt;
    }

    public void setPrompt(String prompt) {
        this.prompt = prompt;
    }

    public String getContext() {
        return context;
    }

    public void setContext(String context) {
        this.context = context;
    }

    public Integer getMaxDepth() {
        return maxDepth;
    }

    public void setMaxDepth(Integer maxDepth) {
        this.maxDepth = maxDepth;
    }

    public Integer getBranchingFactor() {
        return branchingFactor;
    }

    public void setBranchingFactor(Integer branchingFactor) {
        this.branchingFactor = branchingFactor;
    }
}