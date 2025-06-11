package com.tot.controller;

import com.tot.entity.Schedule;
import com.tot.service.ScheduleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/schedule")
@Tag(name = "Schedule", description = "Manage Tree of Thought execution schedules")
public class ScheduleController {

    private static final Logger logger = LoggerFactory.getLogger(ScheduleController.class);

    private final ScheduleService scheduleService;

    @Autowired
    public ScheduleController(ScheduleService scheduleService) {
        this.scheduleService = scheduleService;
    }

    @PostMapping("/create")
    @Operation(summary = "Create schedule", 
               description = "Create a new schedule for Tree of Thought execution with specific target date and historical comparison period")
    public ResponseEntity<Schedule> createSchedule(
            @RequestParam String treeId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime targetDateTime,
            @RequestParam(defaultValue = "1") Integer comparisonDays) {
        
        logger.info("Received request to create schedule for treeId: {} at {} with {}-day comparison", 
                   treeId, targetDateTime, comparisonDays);
        
        try {
            Schedule schedule = scheduleService.createSchedule(treeId, targetDateTime, comparisonDays);
            return ResponseEntity.ok(schedule);
            
        } catch (IllegalArgumentException e) {
            logger.error("Invalid request for creating schedule: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
            
        } catch (Exception e) {
            logger.error("Error creating schedule: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/all")
    @Operation(summary = "Get all schedules", description = "Retrieve all existing schedules")
    public ResponseEntity<List<Schedule>> getAllSchedules() {
        logger.info("Received request to get all schedules");
        
        try {
            List<Schedule> schedules = scheduleService.getAllSchedules();
            return ResponseEntity.ok(schedules);
            
        } catch (Exception e) {
            logger.error("Error retrieving schedules: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/{scheduleId}")
    @Operation(summary = "Get schedule by ID", description = "Retrieve a specific schedule by its ID")
    public ResponseEntity<Schedule> getScheduleById(@PathVariable String scheduleId) {
        logger.info("Received request to get schedule: {}", scheduleId);
        
        try {
            Optional<Schedule> schedule = scheduleService.getScheduleById(scheduleId);
            
            if (schedule.isPresent()) {
                return ResponseEntity.ok(schedule.get());
            } else {
                return ResponseEntity.notFound().build();
            }
            
        } catch (Exception e) {
            logger.error("Error retrieving schedule {}: {}", scheduleId, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PutMapping("/{scheduleId}/status")
    @Operation(summary = "Update schedule status", description = "Update the status of an existing schedule")
    public ResponseEntity<Schedule> updateScheduleStatus(
            @PathVariable String scheduleId,
            @RequestParam String status) {
        
        logger.info("Received request to update schedule {} status to {}", scheduleId, status);
        
        try {
            Schedule updatedSchedule = scheduleService.updateScheduleStatus(scheduleId, status);
            return ResponseEntity.ok(updatedSchedule);
            
        } catch (IllegalArgumentException e) {
            logger.error("Schedule not found: {}", scheduleId);
            return ResponseEntity.notFound().build();
            
        } catch (Exception e) {
            logger.error("Error updating schedule {}: {}", scheduleId, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @DeleteMapping("/{scheduleId}")
    @Operation(summary = "Delete schedule", description = "Delete an existing schedule")
    public ResponseEntity<String> deleteSchedule(@PathVariable String scheduleId) {
        logger.info("Received request to delete schedule: {}", scheduleId);
        
        try {
            scheduleService.deleteSchedule(scheduleId);
            return ResponseEntity.ok("Schedule deleted successfully: " + scheduleId);
            
        } catch (IllegalArgumentException e) {
            logger.error("Schedule not found: {}", scheduleId);
            return ResponseEntity.notFound().build();
            
        } catch (Exception e) {
            logger.error("Error deleting schedule {}: {}", scheduleId, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
}