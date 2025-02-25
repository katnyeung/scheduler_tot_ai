package com.tot.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.tot.entity.Action;
import com.tot.entity.Schedule;
import com.tot.repository.ScheduleRepository;
import com.tot.repository.TotNodeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class ScheduleService {

    private static final Logger logger = LoggerFactory.getLogger(ScheduleService.class);

    @Autowired
    private ScheduleRepository scheduleRepository;

    @Autowired
    private TotNodeRepository totNodeRepository;

    @Autowired
    private TotService totService;

    @Autowired
    private LLMService llmService;

    @Autowired
    private ActionService actionService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Create a new schedule for evaluating a ToT tree
     */
    @Transactional
    public Schedule createSchedule(LocalDateTime scheduledTime, String treeId, Action action) {
        Schedule schedule = new Schedule();
        schedule.setId(java.util.UUID.randomUUID().toString());
        schedule.setScheduledTime(scheduledTime);
        schedule.setTargetNodeId(treeId); // Using targetNodeId to store treeId
        schedule.setAction(action);
        schedule.setStatus("PENDING");

        return scheduleRepository.save(schedule);
    }

    /**
     * Process schedules for the current time
     * Called by the Scheduler
     */
    @Transactional
    public int processSchedulesForCurrentTime() {
        logger.info("Processing schedules for current time: {}", LocalDateTime.now());

        // Find schedules that are due now (within a small window)
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime fiveMinutesAgo = now.minusMinutes(5);
        LocalDateTime oneMinuteFuture = now.plusMinutes(1);

        List<Schedule> dueSchedules = scheduleRepository.findByScheduledTimeBetweenAndStatus(
                fiveMinutesAgo, oneMinuteFuture, "PENDING");

        logger.info("Found {} due schedules to process", dueSchedules.size());

        int processedCount = 0;
        for (Schedule schedule : dueSchedules) {
            try {
                boolean processed = processSchedule(schedule);
                if (processed) {
                    processedCount++;
                }
            } catch (Exception e) {
                logger.error("Error processing schedule {}: {}", schedule.getId(), e.getMessage(), e);
                updateScheduleStatus(schedule.getId(), "ERROR");
            }
        }

        return processedCount;
    }

    /**
     * Process a single schedule
     * @return true if processing was successful, false otherwise
     */
    @Transactional
    public boolean processSchedule(Schedule schedule) {
        logger.info("Processing schedule {} for tree {}", schedule.getId(), schedule.getTargetNodeId());

        try {
            // Mark as in-progress
            schedule.setStatus("IN_PROGRESS");
            scheduleRepository.save(schedule);

            // Get the tree ID from the schedule
            String treeId = schedule.getTargetNodeId();

            // Retrieve the entire tree from TotService
            String treeJson = totService.exportTree(treeId);

            // Parse the tree JSON
            JsonNode treeNode = objectMapper.readTree(treeJson);

            // Check if export was successful
            if (!"success".equals(treeNode.path("status").asText())) {
                logger.error("Failed to export tree {}: {}", treeId, treeNode.path("message").asText());
                updateScheduleStatus(schedule.getId(), "ERROR");
                return false;
            }

            // Extract the tree content
            JsonNode treeContent = treeNode.path("tree");
            String treeContentJson = objectMapper.writeValueAsString(treeContent);

            // Validate the tree with LLM
            boolean isValid = llmService.validateTot(treeContentJson);

            if (!isValid) {
                logger.warn("Tree {} was invalid according to LLM validation", treeId);
                updateScheduleStatus(schedule.getId(), "INVALID_TREE");
                return false;
            }

            logger.info("Tree {} is valid, executing action", treeId);

            // Get the action from the schedule
            Action action = schedule.getAction();

            // Create execution context with the tree ID
            ObjectNode executionContext = objectMapper.createObjectNode();
            executionContext.put("treeId", treeId);
            executionContext.put("validationResult", "valid");

            // Execute the action
            actionService.executeAction(action, executionContext);

            // Mark schedule as completed
            updateScheduleStatus(schedule.getId(), "COMPLETED");
            logger.info("Schedule {} completed successfully", schedule.getId());

            return true;

        } catch (Exception e) {
            logger.error("Error in schedule processing: {}", e.getMessage(), e);
            updateScheduleStatus(schedule.getId(), "ERROR");
            return false;
        }
    }

    /**
     * Update the status of a schedule
     */
    @Transactional
    public Schedule updateScheduleStatus(String scheduleId, String status) {
        Optional<Schedule> scheduleOpt = scheduleRepository.findById(scheduleId);

        if (scheduleOpt.isPresent()) {
            Schedule schedule = scheduleOpt.get();
            schedule.setStatus(status);
            return scheduleRepository.save(schedule);
        }

        throw new IllegalArgumentException("Schedule not found with id: " + scheduleId);
    }

    /**
     * Find schedules for a specific tree
     */
    public List<Schedule> getSchedulesByTreeId(String treeId) {
        return scheduleRepository.findByTargetNodeId(treeId);
    }

    /**
     * Get all pending schedules
     */
    public List<Schedule> getAllPendingSchedules() {
        return scheduleRepository.findByStatus("PENDING");
    }

    /**
     * Create a recurring schedule (to be implemented with a proper scheduling framework)
     */
    public Schedule createRecurringSchedule(String cronExpression, String treeId, Action action) throws JsonProcessingException {
        // Create a schedule for the first occurrence
        LocalDateTime firstOccurrence = LocalDateTime.now().plusMinutes(1);
        Schedule schedule = createSchedule(firstOccurrence, treeId, action);

        // Store cron information in action data for future reference
        JsonNode actionData;
        try {
            actionData = objectMapper.readTree(action.getActionData());
        } catch (Exception e) {
            actionData = objectMapper.createObjectNode();
        }

        // Add cron expression to action data
        ((ObjectNode) actionData).put("cronExpression", cronExpression);
        ((ObjectNode) actionData).put("isRecurring", true);

        action.setActionData(objectMapper.writeValueAsString(actionData));
        actionService.updateAction(action);

        return schedule;
    }

    /**
     * Get count of scheduled jobs by status
     */
    public Map<String, Long> getScheduleStatusCounts() {
        Map<String, Long> counts = new HashMap<>();

        counts.put("PENDING", scheduleRepository.countByStatus("PENDING"));
        counts.put("IN_PROGRESS", scheduleRepository.countByStatus("IN_PROGRESS"));
        counts.put("COMPLETED", scheduleRepository.countByStatus("COMPLETED"));
        counts.put("ERROR", scheduleRepository.countByStatus("ERROR"));
        counts.put("TOTAL", scheduleRepository.count());

        return counts;
    }
}