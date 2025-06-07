package com.tot.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Entity to store Tree of Thought evaluation logs
 */
@Entity
@Table(name = "tot_logs")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TotLog {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    private String treeId;

    private String validationResult;

    @Column(columnDefinition = "TEXT")
    private String validationCriteria;

    @Column(columnDefinition = "TEXT")
    private String treeJson;

    @Column(columnDefinition = "TIMESTAMP")
    private LocalDateTime timestamp;

    @PrePersist
    protected void onCreate() {
        timestamp = LocalDateTime.now();
    }
}