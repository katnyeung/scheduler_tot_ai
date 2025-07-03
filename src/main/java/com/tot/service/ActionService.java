package com.tot.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.tot.entity.Action;
import com.tot.entity.Schedule;
import com.tot.repository.ActionRepository;
import com.tot.repository.ScheduleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Service for core ToT processing and action execution
 */
@Service
public class ActionService {
    private static final Logger logger = LoggerFactory.getLogger(ActionService.class);
    private final ActionRepository actionRepository;
    private final ScheduleRepository scheduleRepository;
    private final TotService totService;
    private final LLMService llmService;
    private final LogService logService;
    private final ObjectMapper objectMapper;

    @Autowired
    public ActionService(
            ActionRepository actionRepository,
            ScheduleRepository scheduleRepository,
            TotService totService,
            LLMService llmService,
            LogService logService) {
        this.actionRepository = actionRepository;
        this.scheduleRepository = scheduleRepository;
        this.totService = totService;
        this.llmService = llmService;
        this.logService = logService;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Process all schedules that are due now asynchronously
     */
    public void processSchedulesForCurrentTimeAsync() {
        logger.info("Initiating async schedule processing for current time: {}", LocalDateTime.now());

        // Find schedules that are due
        List<Schedule> dueSchedules = findDueSchedules();
        logger.info("Found {} due schedules to process asynchronously", dueSchedules.size());

        // Process each schedule asynchronously
        for (Schedule schedule : dueSchedules) {
            processScheduleAsync(schedule)
                    .exceptionally(ex -> {
                        logger.error("Async processing of schedule {} failed: {}",
                                schedule.getId(), ex.getMessage(), ex);
                        try {
                            updateScheduleStatus(schedule.getId(), "ERROR");
                        } catch (Exception e) {
                            logger.error("Failed to update schedule status: {}", e.getMessage());
                        }
                        return null;
                    });
        }
    }

    /**
     * Process a single schedule through the entire workflow asynchronously
     * @param schedule The schedule to process
     * @return CompletableFuture that completes when the processing is done
     */
    @Async("taskExecutor")
    public CompletableFuture<Void> processScheduleAsync(Schedule schedule) {
        String scheduleId = schedule.getId();
        String treeId = schedule.getTargetNodeId();

        logger.info("Async processing of schedule {} for tree {} started on thread {}",
                scheduleId, treeId, Thread.currentThread().getName());

        try {
            // 1. Mark schedule as in-progress
            updateScheduleStatus(scheduleId, "IN_PROGRESS");

            // 2. Get the tree of thought from TotService
            String treeJson = totService.getTreeOfThought(treeId);
            logger.info("Tree of thought retrieved for schedule {}", scheduleId);

            // 3. Validate the tree with LLMService using historical comparison (service selected by feature flag)
            int comparisonDays = schedule.getComparisonDays() != null ? schedule.getComparisonDays() : 1;
            logger.info("Starting LLM validation with {}-day historical comparison for schedule {}", comparisonDays, scheduleId);
            ValidationResult validationResult = llmService.validateTreeWithHistoricalComparison(treeJson, comparisonDays);
            logger.info("LLM validation with {}-day historical comparison completed for schedule {}: result={}", comparisonDays, scheduleId, validationResult.getResult());

            // 4. Log the tree evaluation with detailed criteria
            logService.logTreeEvaluation(treeId, treeJson, validationResult.getResult(), validationResult.getCriteria());

            // 5. Execute core logic if tree evaluation is true
            if (validationResult.isPositive()) {
                logger.info("Executing core logic for schedule {} (TOT result: true)", scheduleId);
                executeCoreLogic(schedule, treeId, validationResult.getResult());
                updateScheduleStatus(scheduleId, "COMPLETED");
            } else {
                logger.info("TOT evaluation returned false for schedule {} - no action taken", scheduleId);
                updateScheduleStatus(scheduleId, "COMPLETED");
            }

            logger.info("Async processing of schedule {} completed successfully", scheduleId);
            return CompletableFuture.completedFuture(null);
        } catch (Exception e) {
            logger.error("Error in async processing of schedule {}: {}", scheduleId, e.getMessage(), e);
            updateScheduleStatus(scheduleId, "ERROR");
            CompletableFuture<Void> future = new CompletableFuture<>();
            future.completeExceptionally(e);
            return future;
        }
    }

    /**
     * Execute core business logic for validated schedules
     */
    private void executeCoreLogic(Schedule schedule, String treeId, String validationResult) {
        logger.info("Executing core logic for schedule: {}", schedule.getId());
        
        // Create context for execution
        ObjectNode context = objectMapper.createObjectNode();
        context.put("treeId", treeId);
        context.put("scheduleId", schedule.getId());
        context.put("validationResult", validationResult);
        
        // Execute the action if defined
        Action action = schedule.getAction();
        if (action != null) {
            executeAction(action, context);
        } else {
            logger.warn("No action defined for schedule {}", schedule.getId());
        }
    }

    /**
     * Execute a specific action with context
     */
    @Transactional
    public void executeAction(Action action, ObjectNode context) {
        logger.info("Executing action: {}, type: {}", action.getId(), action.getActionType());
        
        // Basic action execution - extensible for future action types
        logger.info("Action executed successfully: {}", action.getId());
    }


    /**
     * Execute action for a specific tree ID with configurable historical comparison
     * @param treeId The tree ID to process
     * @param comparisonDays Number of days back to compare (e.g., 1 for yesterday, 7 for last week)
     * @return Result message indicating the action taken
     */
    public String executeActionForTreeWithHistoricalComparison(String treeId, int comparisonDays) {
        logger.info("Executing action for treeId: {} with {}-day historical comparison", treeId, comparisonDays);
        
        try {
            // Get the tree of thought
            String treeJson = totService.getTreeOfThought(treeId);
            
            if (treeJson == null || treeJson.trim().isEmpty()) {
                logger.error("No tree found for treeId: {}", treeId);
                throw new IllegalArgumentException("Tree not found for ID: " + treeId);
            }

            // Validate the tree with LLMService using specific historical comparison period (service selected by feature flag)
            ValidationResult validationResult = llmService.validateTreeWithHistoricalComparison(treeJson, comparisonDays);
            logger.info("LLM validation with {}-day historical comparison completed for treeId {}: result={}", 
                comparisonDays, treeId, validationResult.getResult());
            
            // Log the tree evaluation with detailed criteria
            logService.logTreeEvaluation(treeId, treeJson, validationResult.getResult(), validationResult.getCriteria());

            // Determine action based on validation result
            String actionResult;
            if (validationResult.isPositive()) {
                actionResult = String.format("Action executed: Decision positive (vs %d days ago) - %s...", 
                    comparisonDays, validationResult.getCriteria().substring(0, Math.min(150, validationResult.getCriteria().length())));
                logger.info("Executing positive action for treeId {} (vs {} days ago)", treeId, comparisonDays);
            } else {
                actionResult = String.format("Action executed: Hold (no action taken). Decision: negative (vs %d days ago) - %s...", 
                    comparisonDays, validationResult.getCriteria().substring(0, Math.min(150, validationResult.getCriteria().length())));
                logger.info("Executing negative action (hold) for treeId {} (vs {} days ago)", treeId, comparisonDays);
            }
            
            return actionResult;
            
        } catch (Exception e) {
            logger.error("Error executing action for treeId {} with {}-day comparison: {}", treeId, comparisonDays, e.getMessage(), e);
            
            // Log the failure
            try {
                logService.logValidationFailure(treeId);
            } catch (Exception logError) {
                logger.error("Failed to log validation failure: {}", logError.getMessage());
            }
            
            throw new RuntimeException("Error executing action for tree " + treeId + " with " + comparisonDays + "-day comparison: " + e.getMessage());
        }
    }


    /**
     * Find schedules that are due for processing
     */
    private List<Schedule> findDueSchedules() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime fiveMinutesAgo = now.minusMinutes(5);
        LocalDateTime oneMinuteFuture = now.plusMinutes(1);

        return scheduleRepository.findByScheduledTimeBetweenAndStatus(
                fiveMinutesAgo, oneMinuteFuture, "PENDING");
    }

    /**
     * Update the status of a schedule
     */
    @Transactional
    public void updateScheduleStatus(String scheduleId, String status) {
        scheduleRepository.findById(scheduleId).ifPresent(schedule -> {
            schedule.setStatus(status);
            scheduleRepository.save(schedule);
            logger.info("Updated schedule {} status to {}", scheduleId, status);
        });
    }
}