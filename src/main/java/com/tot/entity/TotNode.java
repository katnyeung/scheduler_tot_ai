package com.tot.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TotNode {
    @Id
    private String nodeId;

    private String treeId;
    private String content;  // Content of the node (e.g., description/details)
    private String criteria;   // Prompt used for LLM evaluation

    @ElementCollection
    @CollectionTable(name = "node_children", joinColumns = @JoinColumn(name = "node_id"))
    @MapKeyColumn(name = "branch_key")
    @Column(name = "child_node_id")
    private Map<String, String> children; // Mapping of branch keys (e.g., "yes", "no") to child nodeIds
}