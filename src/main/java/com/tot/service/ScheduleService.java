package com.tot.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Service for delegating schedule processing to ActionService
 */
@Service
public class ScheduleService {
    private static final Logger logger = LoggerFactory.getLogger(ScheduleService.class);

    private final ActionService actionService;

    @Autowired
    public ScheduleService(ActionService actionService) {
        this.actionService = actionService;
    }

    /**
     * Process all schedules that are due now asynchronously
     * Delegates to ActionService for core processing logic
     */
    public void processSchedulesForCurrentTimeAsync() {
        logger.info("Delegating schedule processing to ActionService");
        actionService.processSchedulesForCurrentTimeAsync();
    }
}