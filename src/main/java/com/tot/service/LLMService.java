package com.tot.service;

/**
 * LLM Service interface for Tree of Thought operations
 * Implementations are selected via feature flag in application.properties
 */
public interface LLMService {
    
    /**
     * Generate a Tree of Thought structure in JSON format
     * @param prompt The prompt to generate a tree from
     * @return JSON string representation of the generated tree
     */
    String generateTreeOfThought(String prompt);

    /**
     * Refine an existing Tree of Thought with new requirements
     * @param treeJson The JSON representation of the existing tree
     * @param newPrompt The new prompt requirement to guide refinement
     * @return JSON string representation of the refined tree
     */
    String refineTreeOfThought(String treeJson, String newPrompt);

    /**
     * Validate a Tree of Thought structure with historical comparison
     * @param treeJson JSON representation of the tree
     * @param comparisonDays Number of days back to compare
     * @return ValidationResult containing both decision and detailed criteria
     */
    ValidationResult validateTreeWithHistoricalComparison(String treeJson, int comparisonDays);

    /**
     * Legacy method for backward compatibility
     * @param treeJson JSON representation of the tree
     * @return Simple validation result string
     */
    String validateTree(String treeJson);
}