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

    @ManyToOne
    @JoinColumn(name = "action_id")
    private Action action; // Action to perform when scheduled
}