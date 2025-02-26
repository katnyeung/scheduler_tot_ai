package com.tot.controller;

import com.tot.entity.TotLog;
import com.tot.service.LogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/logs")
@Tag(name = "Logs", description = "Access Tree of Thought evaluation logs")
public class LogController {

    private final LogService logService;

    @Autowired
    public LogController(LogService logService) {
        this.logService = logService;
    }

    @GetMapping("/tree/{treeId}")
    @Operation(summary = "Get tree logs", description = "Retrieve recent logs for a specific tree")
    public ResponseEntity<List<TotLog>> getTreeLogs(@PathVariable String treeId) {
        List<TotLog> logs = logService.getRecentLogsForTree(treeId);
        return ResponseEntity.ok(logs);
    }

    @GetMapping("/timeperiod")
    @Operation(summary = "Get logs by time period", description = "Retrieve logs from a specific time period")
    public ResponseEntity<List<TotLog>> getLogsByTimePeriod(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
        List<TotLog> logs = logService.getLogsBetweenDates(start, end);
        return ResponseEntity.ok(logs);
    }

    @GetMapping("/stats")
    @Operation(summary = "Get validation statistics", description = "Retrieve statistics about tree validations")
    public ResponseEntity<LogService.ValidateStats> getValidationStats() {
        LogService.ValidateStats stats = logService.getValidationStats();
        return ResponseEntity.ok(stats);
    }
}