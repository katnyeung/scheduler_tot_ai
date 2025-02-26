package com.tot.repository;

import com.tot.entity.TotLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository for accessing Tree of Thought logs
 */
@Repository
public interface TotLogRepository extends JpaRepository<TotLog, String> {
    /**
     * Find logs for a specific tree
     */
    List<TotLog> findByTreeId(String treeId);

    /**
     * Find logs by validation result
     */
    List<TotLog> findByValidationResult(String validationResult);

    /**
     * Find logs created within a specific time period
     */
    List<TotLog> findByTimestampBetween(LocalDateTime start, LocalDateTime end);

    /**
     * Find most recent logs for a tree
     */
    List<TotLog> findTop10ByTreeIdOrderByTimestampDesc(String treeId);

    /**
     * Count logs by validation result
     */
    long countByValidationResult(String validationResult);
}