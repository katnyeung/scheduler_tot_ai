package com.tot.service;

import com.tot.entity.TotLog;
import com.tot.repository.TotLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Service for logging Tree of Thought evaluations and results
 */
@Service
public class LogService {
    private static final Logger logger = LoggerFactory.getLogger(LogService.class);

    private final TotLogRepository totLogRepository;

    @Autowired
    public LogService(TotLogRepository totLogRepository) {
        this.totLogRepository = totLogRepository;
    }

    /**
     * Log a Tree of Thought evaluation including the tree content and validation result
     * @param treeId ID of the evaluated tree
     * @param treeJson JSON representation of the tree
     * @param validationResult Result of tree validation (true/false)
     * @return The created log entry
     */
    @Transactional
    public TotLog logTreeEvaluation(String treeId, String treeJson, String validationResult) {
        logger.info("Tree Evaluation - ID: {}, Result: {}, Time: {}",
                treeId, validationResult, LocalDateTime.now());

        // Log the full tree content at debug level to avoid cluttering logs
        logger.debug("Tree Content for {}: {}", treeId, treeJson);

        // Create and save log entry
        TotLog logEntry = new TotLog();
        logEntry.setTreeId(treeId);
        logEntry.setTreeJson(treeJson);
        logEntry.setValidationResult(validationResult);

        return totLogRepository.save(logEntry);
    }

    /**
     * Log validation failure for a tree
     * @param treeId ID of the tree that failed validation
     * @return The created log entry
     */
    @Transactional
    public TotLog logValidationFailure(String treeId) {
        logger.warn("Tree validation failed for tree: {}", treeId);

        // Create and save log entry
        TotLog logEntry = new TotLog();
        logEntry.setTreeId(treeId);
        logEntry.setValidationResult("false");

        return totLogRepository.save(logEntry);
    }

    /**
     * Get recent logs for a specific tree
     * @param treeId ID of the tree
     * @return List of recent log entries
     */
    public List<TotLog> getRecentLogsForTree(String treeId) {
        return totLogRepository.findTop10ByTreeIdOrderByTimestampDesc(treeId);
    }

    /**
     * Get logs from a specific time period
     * @param start Start time
     * @param end End time
     * @return List of log entries in the time period
     */
    public List<TotLog> getLogsBetweenDates(LocalDateTime start, LocalDateTime end) {
        return totLogRepository.findByTimestampBetween(start, end);
    }

    /**
     * Get validation statistics
     * @return Object with counts of true and false results
     */
    public ValidateStats getValidationStats() {
        ValidateStats stats = new ValidateStats();
        stats.setValidCount(totLogRepository.countByValidationResult("true"));
        stats.setInvalidCount(totLogRepository.countByValidationResult("false"));
        stats.setTotalCount(totLogRepository.count());
        return stats;
    }

    /**
     * Inner class for validation statistics
     */
    public static class ValidateStats {
        private long validCount;
        private long invalidCount;
        private long totalCount;

        public long getValidCount() {
            return validCount;
        }

        public void setValidCount(long validCount) {
            this.validCount = validCount;
        }

        public long getInvalidCount() {
            return invalidCount;
        }

        public void setInvalidCount(long invalidCount) {
            this.invalidCount = invalidCount;
        }

        public long getTotalCount() {
            return totalCount;
        }

        public void setTotalCount(long totalCount) {
            this.totalCount = totalCount;
        }

        public double getValidPercentage() {
            return totalCount > 0 ? (double) validCount / totalCount * 100 : 0;
        }
    }
}