package com.tot.scheduler;

import com.tot.service.ScheduleService;
import com.tot.service.ActionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Scheduler component that triggers scheduled ToT evaluations asynchronously
 */
@Component
public class Scheduler {
    private static final Logger logger = LoggerFactory.getLogger(Scheduler.class);
    private final ScheduleService scheduleService;
    private final ActionService actionService;

    @Autowired
    public Scheduler(ScheduleService scheduleService, ActionService actionService) {
        this.scheduleService = scheduleService;
        this.actionService = actionService;
    }

    /**
     * Scheduled method that runs based on the configured cron expression
     * Default is every 5 minutes
     */
    //@Scheduled(cron = "${tot.scheduler.cron:0 */5 * * * *}")
    public void trigger() {
        logger.info("Scheduler triggered at {}", LocalDateTime.now());

        try {
            // Process all due schedules asynchronously
            scheduleService.processSchedulesForCurrentTimeAsync();
            logger.info("Schedule processing initiated asynchronously and will continue in background");
        } catch (Exception e) {
            logger.error("Error in scheduler execution: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Execute action for a specific tree ID using ActionService
     * This provides an alternative execution path that can be used 
     * independently of the scheduled workflow
     * @param treeId The tree ID to execute action for
     */
    public void executeActionForTree(String treeId) {
        logger.info("Scheduler executing action for treeId {} at {}", treeId, LocalDateTime.now());
        
        try {
            // Use ActionService directly to execute the action
            String result = actionService.executeActionForTree(treeId);
            logger.info("ActionService execution completed for treeId {}: {}", treeId, result);
        } catch (Exception e) {
            logger.error("Error executing action via ActionService for treeId {}: {}", 
                        treeId, e.getMessage(), e);
        }
    }
}