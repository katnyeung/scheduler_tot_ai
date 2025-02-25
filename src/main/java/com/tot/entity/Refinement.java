package com.tot.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Refinement {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    private String treeId; // Reference to the tree being refined

    @ManyToOne
    @JoinColumn(name = "action_id")
    private Action refinementAction; // Action to perform for refinement

    @Column(columnDefinition = "TIMESTAMP")
    private LocalDateTime createdAt;

    private String status; // e.g., "PENDING", "IN_PROGRESS", "COMPLETED"

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}