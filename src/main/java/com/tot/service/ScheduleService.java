package com.tot.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.tot.entity.Schedule;
import com.tot.repository.ScheduleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

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
     * Process all schedules that are due now
     * @return Number of schedules processed
     */
    @Transactional
    public int processSchedulesForCurrentTime() {
        logger.info("Processing schedules for current time: {}", LocalDateTime.now());

        // Find schedules that are due
        List<Schedule> dueSchedules = findDueSchedules();
        logger.info("Found {} due schedules", dueSchedules.size());

        // Process each schedule
        int processed = 0;
        for (Schedule schedule : dueSchedules) {
            try {
                processSchedule(schedule);
                processed++;
            } catch (Exception e) {
                logger.error("Error processing schedule {}: {}", schedule.getId(), e.getMessage());
                updateScheduleStatus(schedule.getId(), "ERROR");
            }
        }

        return processed;
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
     * Process a single schedule through the entire workflow
     * @param schedule The schedule to process
     */
    @Transactional
    public void processSchedule(Schedule schedule) {
        String scheduleId = schedule.getId();
        String treeId = schedule.getTargetNodeId();

        logger.info("Processing schedule {} for tree {}", scheduleId, treeId);

        // 1. Mark schedule as in-progress
        updateScheduleStatus(scheduleId, "IN_PROGRESS");

        // 2. Get the tree of thought from TotService
        String treeJson = totService.getTreeOfThought(treeId);

        // 3. Validate the tree with LLMService
        String validationResult = llmService.validateTree(treeJson);

        // 4. Log the tree evaluation and save to repository
        logService.logTreeEvaluation(treeId, treeJson, validationResult);

        // 5. Execute action if tree is valid
        if ("VALID".equals(validationResult)) {
            // Create context for action execution
            ObjectNode context = objectMapper.createObjectNode();
            context.put("treeId", treeId);
            context.put("scheduleId", scheduleId);
            context.put("validationResult", validationResult);

            // Execute the action
            actionService.executeAction(schedule.getAction(), context);

            // Update schedule status
            updateScheduleStatus(scheduleId, "COMPLETED");
        } else {
            // Log validation failure and save to repository
            logService.logValidationFailure(treeId);

            // Update schedule status
            updateScheduleStatus(scheduleId, "FAILED");
        }
    }

    /**
     * Update the status of a schedule
     * @param scheduleId ID of the schedule to update
     * @param status New status value
     */
    private void updateScheduleStatus(String scheduleId, String status) {
        scheduleRepository.findById(scheduleId).ifPresent(schedule -> {
            schedule.setStatus(status);
            scheduleRepository.save(schedule);
            logger.info("Updated schedule {} status to {}", scheduleId, status);
        });
    }
}