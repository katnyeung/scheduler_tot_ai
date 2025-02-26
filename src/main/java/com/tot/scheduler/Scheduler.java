package com.tot.scheduler;

import com.tot.service.ScheduleService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Scheduler component that triggers scheduled ToT evaluations
 */
@Component
public class Scheduler {
    private static final Logger logger = LoggerFactory.getLogger(Scheduler.class);
    private final ScheduleService scheduleService;

    @Autowired
    public Scheduler(ScheduleService scheduleService) {
        this.scheduleService = scheduleService;
    }

    /**
     * Scheduled method that runs based on the configured cron expression
     * Default is every 5 minutes
     */
    @Scheduled(cron = "${tot.scheduler.cron:0 */5 * * * *}")
    public void trigger() {
        logger.info("Scheduler triggered at {}", LocalDateTime.now());

        try {
            // Process all due schedules
            int processed = scheduleService.processSchedulesForCurrentTime();
            logger.info("Processed {} scheduled jobs", processed);
        } catch (Exception e) {
            logger.error("Error in scheduler execution: {}", e.getMessage(), e);
        }
    }
}