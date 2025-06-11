package com.tot.service;

import com.tot.entity.Action;
import com.tot.entity.Schedule;
import com.tot.repository.ActionRepository;
import com.tot.repository.ScheduleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Service for managing schedules and delegating schedule processing
 */
@Service
public class ScheduleService {
    private static final Logger logger = LoggerFactory.getLogger(ScheduleService.class);

    private final ActionService actionService;
    private final ScheduleRepository scheduleRepository;
    private final ActionRepository actionRepository;

    @Autowired
    public ScheduleService(ActionService actionService, ScheduleRepository scheduleRepository, ActionRepository actionRepository) {
        this.actionService = actionService;
        this.scheduleRepository = scheduleRepository;
        this.actionRepository = actionRepository;
    }

    /**
     * Process all schedules that are due now asynchronously
     * Delegates to ActionService for core processing logic
     */
    public void processSchedulesForCurrentTimeAsync() {
        logger.info("Delegating schedule processing to ActionService");
        actionService.processSchedulesForCurrentTimeAsync();
    }

    /**
     * Create a new schedule for a specific tree ID with target date and comparison period
     * @param treeId The tree ID to schedule
     * @param targetDateTime The target date and time for execution
     * @param comparisonDays Number of days back to compare for historical analysis
     * @return The created schedule
     */
    @Transactional
    public Schedule createSchedule(String treeId, LocalDateTime targetDateTime, Integer comparisonDays) {
        logger.info("Creating schedule for treeId: {} at {} with {}-day comparison", treeId, targetDateTime, comparisonDays);

        // Validate inputs
        if (treeId == null || treeId.trim().isEmpty()) {
            throw new IllegalArgumentException("Tree ID cannot be null or empty");
        }
        if (targetDateTime == null) {
            throw new IllegalArgumentException("Target date time cannot be null");
        }
        if (targetDateTime.isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Target date time cannot be in the past");
        }
        if (comparisonDays == null || comparisonDays < 1 || comparisonDays > 365) {
            throw new IllegalArgumentException("Comparison days must be between 1 and 365");
        }

        Schedule schedule = new Schedule();
        schedule.setTargetNodeId(treeId);
        schedule.setScheduledTime(targetDateTime);
        schedule.setComparisonDays(comparisonDays);
        schedule.setStatus("PENDING");

        // Create a default action if none exists
        Action action = new Action();
        action.setActionType("TOT_EVALUATION");
        action.setActionData("{\"description\":\"Tree of Thought evaluation with " + comparisonDays + "-day historical comparison\"}");
        Action savedAction = actionRepository.save(action);
        schedule.setAction(savedAction);

        Schedule savedSchedule = scheduleRepository.save(schedule);
        logger.info("Created schedule with ID: {} for treeId: {}", savedSchedule.getId(), treeId);

        return savedSchedule;
    }

    /**
     * Get all schedules
     * @return List of all schedules
     */
    public List<Schedule> getAllSchedules() {
        logger.info("Retrieving all schedules");
        return scheduleRepository.findAll();
    }

    /**
     * Get schedule by ID
     * @param scheduleId The schedule ID
     * @return The schedule if found
     */
    public Optional<Schedule> getScheduleById(String scheduleId) {
        logger.info("Retrieving schedule with ID: {}", scheduleId);
        return scheduleRepository.findById(scheduleId);
    }

    /**
     * Update schedule status
     * @param scheduleId The schedule ID
     * @param status The new status
     * @return Updated schedule
     */
    @Transactional
    public Schedule updateScheduleStatus(String scheduleId, String status) {
        logger.info("Updating schedule {} status to {}", scheduleId, status);
        
        Schedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new IllegalArgumentException("Schedule not found: " + scheduleId));
        
        schedule.setStatus(status);
        return scheduleRepository.save(schedule);
    }

    /**
     * Delete a schedule
     * @param scheduleId The schedule ID to delete
     */
    @Transactional
    public void deleteSchedule(String scheduleId) {
        logger.info("Deleting schedule with ID: {}", scheduleId);
        
        if (!scheduleRepository.existsById(scheduleId)) {
            throw new IllegalArgumentException("Schedule not found: " + scheduleId);
        }
        
        scheduleRepository.deleteById(scheduleId);
        logger.info("Successfully deleted schedule: {}", scheduleId);
    }
}