package com.tot.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.tot.entity.Action;
import com.tot.entity.Schedule;
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
 * Service for processing scheduled ToT evaluations
 */
@Service
public class ScheduleService {
    private static final Logger logger = LoggerFactory.getLogger(ScheduleService.class);

    private final ScheduleRepository scheduleRepository;
    private final TotService totService;
    private final LLMService llmService;
    private final LogService logService;
    private final ActionService actionService;
    private final ObjectMapper objectMapper;

    @Autowired
    public ScheduleService(
            ScheduleRepository scheduleRepository,
            TotService totService,
            LLMService llmService,
            LogService logService,
            ActionService actionService) {
        this.scheduleRepository = scheduleRepository;
        this.totService = totService;
        this.llmService = llmService;
        this.logService = logService;
        this.actionService = actionService;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Process all schedules that are due now asynchronously
     * This method returns immediately and processing happens in background
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

            // 3. Validate the tree with LLMService (this is the potentially long API call)
            logger.info("Starting LLM validation for schedule {}", scheduleId);
            String validationResult = llmService.validateTree(treeJson);
            logger.info("LLM validation completed for schedule {}: result={}", scheduleId, validationResult);

            // 4. Log the tree evaluation and save to repository
            logService.logTreeEvaluation(treeId, treeJson, validationResult);

            // 5. Execute action if tree evaluation is true
            if ("true".equals(validationResult)) {
                // Create context for action execution
                ObjectNode context = objectMapper.createObjectNode();
                context.put("treeId", treeId);
                context.put("scheduleId", scheduleId);
                context.put("validationResult", validationResult);

                // Execute the action
                logger.info("Executing action for schedule {} (TOT result: true)", scheduleId);
                Action action = schedule.getAction();
                if (action != null) {
                    actionService.executeAction(action, context);
                } else {
                    logger.warn("No action defined for schedule {}", scheduleId);
                }

                // Update schedule status
                updateScheduleStatus(scheduleId, "COMPLETED");
            } else {
                // Log when TOT evaluation is false (no action needed)
                logger.info("TOT evaluation returned false for schedule {} - no action taken", scheduleId);

                // Update schedule status
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
     * Find schedules that are due for processing
     * @return List of due schedules
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
     * @param scheduleId ID of the schedule to update
     * @param status New status value
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