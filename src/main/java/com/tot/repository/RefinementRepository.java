package com.tot.repository;


import com.tot.entity.Refinement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface RefinementRepository extends JpaRepository<Refinement, String> {
    /**
     * Find refinements for a specific tree
     */
    List<Refinement> findByTreeId(String treeId);

    /**
     * Find refinements by status
     */
    List<Refinement> findByStatus(String status);

    /**
     * Find refinements created within a date range with specific status
     */
    List<Refinement> findByCreatedAtBetweenAndStatus(
            LocalDateTime start, LocalDateTime end, String status);

    /**
     * Find most recent refinements for a tree
     */
    List<Refinement> findTop5ByTreeIdOrderByCreatedAtDesc(String treeId);

    /**
     * Count refinements by tree and status
     */
    long countByTreeIdAndStatus(String treeId, String status);
}