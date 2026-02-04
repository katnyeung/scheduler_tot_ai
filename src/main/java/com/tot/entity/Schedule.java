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
public class Schedule {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(columnDefinition = "TIMESTAMP")
    private LocalDateTime scheduledTime;

    private String targetNodeId; // Reference to the TotNode to trigger

    private String status; // e.g., "PENDING", "COMPLETED", "FAILED"

    @Column(columnDefinition = "INTEGER DEFAULT 1")
    private Integer comparisonDays; // Number of days back to compare for historical analysis

    @Column(columnDefinition = "VARCHAR(50) DEFAULT 'stock'")
    private String serviceType; // Type of LLM service to use ("stock", "generic", etc.)

    @ManyToOne
    @JoinColumn(name = "action_id")
    private Action action; // Action to perform when scheduled
}