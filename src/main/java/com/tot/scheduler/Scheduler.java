package com.tot.scheduler;

import com.tot.service.ScheduleService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Scheduler component that triggers ScheduleService to process scheduled trees
 */
@Component
public class Scheduler {

    private static final Logger logger = LoggerFactory.getLogger(Scheduler.class);

    @Autowired
    private ScheduleService scheduleService;

    /**
     * Cron job to trigger schedule processing - runs based on cron expression in properties
     * Default: Every 5 minutes
     */
    @Scheduled(cron = "${tot.scheduler.cron:0 */5 * * * *}")
    public void triggerScheduledJobs() {
        logger.info("Scheduler triggered at {}", LocalDateTime.now());

        try {
            // Call the service to process schedules
            int processedCount = scheduleService.processSchedulesForCurrentTime();
            logger.info("Processed {} scheduled jobs", processedCount);
        } catch (Exception e) {
            logger.error("Error in scheduler execution: {}", e.getMessage(), e);
        }
    }
}