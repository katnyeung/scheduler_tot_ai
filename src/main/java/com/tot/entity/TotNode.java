package com.tot.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Map;
import java.util.Objects;

@Entity
@Data
@NoArgsConstructor
@IdClass(TotNode.TotNodeId.class)
public class TotNode {
    @Id
    private String nodeId;
    
    @Id
    private String treeId;

    private String content;  // Content of the node (e.g., description/details)
    private String criteria;   // Prompt used for LLM evaluation

    @ElementCollection
    @CollectionTable(name = "node_children", 
                     joinColumns = {
                         @JoinColumn(name = "node_id", referencedColumnName = "nodeId"),
                         @JoinColumn(name = "tree_id", referencedColumnName = "treeId")
                     })
    @MapKeyColumn(name = "branch_key")
    @Column(name = "child_node_id")
    private Map<String, String> children; // Mapping of branch keys (e.g., "yes", "no") to child nodeIds

    @Data
    @NoArgsConstructor
    public static class TotNodeId implements Serializable {
        private String nodeId;
        private String treeId;

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            TotNodeId totNodeId = (TotNodeId) o;
            return Objects.equals(nodeId, totNodeId.nodeId) && Objects.equals(treeId, totNodeId.treeId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(nodeId, treeId);
        }
    }
}